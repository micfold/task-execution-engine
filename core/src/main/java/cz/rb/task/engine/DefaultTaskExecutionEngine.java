package cz.rb.task.engine;

import cz.rb.task.api.TaskHandler;
import cz.rb.task.messaging.TaskEvent;
import cz.rb.task.messaging.TaskEventPublisher;
import cz.rb.task.model.Task;
import cz.rb.task.model.TaskResult;
import cz.rb.task.model.TaskStatus;
import cz.rb.task.persistence.TaskEntity;
import cz.rb.task.persistence.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

// TODO: Missing javadoc comments
@Slf4j
@Service
public class DefaultTaskExecutionEngine {
    private final RetryStrategy retryStrategy;
    private final TaskRepository taskRepository;
    private final TaskEventPublisher eventPublisher;

    public DefaultTaskExecutionEngine(
            RetryStrategy retryStrategy,
            TaskRepository taskRepository,
            TaskEventPublisher eventPublisher
    ) {
        this.retryStrategy = retryStrategy;
        this.taskRepository = taskRepository;
        this.eventPublisher = eventPublisher;
    }

    // TODO: Missing javadoc comments
    public Mono<TaskResult> executeTask(final Task task, TaskHandler handler) {
        if (handler == null) {
            return Mono.error(new IllegalArgumentException("Task handler cannot be null"));
        }

        if (task == null || task.taskId() == null || task.type() == null) {
            return Mono.error(new IllegalArgumentException("Invalid task: missing required fields"));
        }

        return markTaskStarted(task)
                .flatMap(t -> executeWithRetry(t, handler))
                .doOnNext(result -> publishTaskEvent(task, result))
                .flatMap(result -> updateTaskWithResult(task, result));
    }

    private Mono<Task> markTaskStarted(final Task task) {
        final Task updatedTask = task.withStatus(TaskStatus.IN_PROGRESS);
        return taskRepository.save(TaskEntity.fromDomain(updatedTask))
                .map(TaskEntity::toDomain)
                .doOnSuccess(t -> eventPublisher.publishEvent(
                        TaskEvent.taskStarted(t.taskId(), t.type())));
    }

    private Mono<TaskResult> executeWithRetry(final Task task, TaskHandler handler) {
        return retryStrategy.executeWithRetry(task,
                t -> handler.execute(t)
                        .timeout(Duration.ofSeconds(5))
                        .doOnSuccess(result -> logTaskSuccess(task))
                        .doOnError(error -> logTaskError(task, error))
        );
    }

    private void logTaskSuccess(final Task task) {
        log.info("Task executed successfully: {}", task.taskId());
    }

    private void logTaskError(final Task task, Throwable error) {
        log.error("Task execution failed: {}, Error: {}", task.taskId(), error.getMessage());
    }

    private Mono<TaskResult> updateTaskWithResult(final Task task, TaskResult result) {
        final TaskStatus newStatus = switch (result) {
            case TaskResult.Success s -> {
                log.debug("Updating to COMPLETED status for task: {}", task.taskId());
                yield TaskStatus.COMPLETED;
            }
            case TaskResult.Failure f -> {
                TaskStatus status = f.retryable() ? TaskStatus.FAILED : TaskStatus.DEAD_LETTER;
                log.debug("Updating status for task: {}, isRetryable: {}, newStatus: {}",
                        task.taskId(), f.retryable(), status);
                yield status;
            }
        };

        final Task updatedTask = task.withStatus(newStatus);
        return taskRepository.save(TaskEntity.fromDomain(updatedTask))
                .doOnNext(savedEntity -> log.debug("Task saved with new status: {}", savedEntity.status()))
                .thenReturn(result);
    }

    private void publishTaskEvent(final Task task, TaskResult result) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("taskType", task.type());
        metadata.put("retryCount", task.retryCount());

        switch (result) {
            case TaskResult.Success s -> metadata.put("result", s.result());
            case TaskResult.Failure f -> {
                metadata.put("error", f.error());
                metadata.put("retryable", f.retryable());
            }
        }

        final String eventType = switch (result) {
            case TaskResult.Success s -> TaskEvent.EventTypes.TASK_COMPLETED;
            case TaskResult.Failure f -> f.retryable() ?
                    TaskEvent.EventTypes.TASK_FAILED : TaskEvent.EventTypes.MOVED_TO_DLQ;
        };

        eventPublisher.publishEvent(new TaskEvent(
                task.taskId(),
                task.type(),
                eventType,
                metadata,
                Instant.now()
        ));
    }
}