package cz.rb.task.messaging;

import cz.rb.task.model.Task;
import cz.rb.task.model.TaskStatus;
import cz.rb.task.persistence.TaskEntity;
import cz.rb.task.persistence.TaskRepository;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles dead letter queue (DLQ) processing for failed tasks.
 * Manages the movement of tasks to DLQ and associated event publishing.
 */
@Slf4j
@Component
public class DlqHandler {
    private final TaskRepository taskRepository;
    private final TaskEventPublisher eventPublisher;
    private final KafkaPublisher<String, Task> kafkaPublisher;

    @Value("${task.kafka.topic.dlq:DEFAULT_KAFKA_TOPIC}")
    private String dlqTopic;

    public DlqHandler(
            TaskRepository taskRepository,
            KafkaPublisher<String, Task> kafkaPublisher,
            TaskEventPublisher eventPublisher
    ) {
        this.taskRepository = taskRepository;
        this.kafkaPublisher = kafkaPublisher;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Handles a task that has failed and needs to be moved to DLQ.
     *
     * @param task The failed task
     * @param error The error that caused the failure
     * @return A Mono that completes when DLQ processing is done
     */
    public Mono<Void> handleDeadLetter(final Task task, Throwable error) {
        if (task == null) {
            return Mono.error(new IllegalArgumentException("Task cannot be null"));
        }
        if (error == null) {
            return Mono.error(new IllegalArgumentException("Error cannot be null"));
        }

        log.error("Moving task to DLQ: {}, Error: {}", task.taskId(), error.getMessage(), error);

        final Task deadLetterTask = task.withStatus(TaskStatus.DEAD_LETTER);
        final TaskEntity taskEntity = TaskEntity.fromDomain(deadLetterTask);

        return taskRepository.save(taskEntity)
                .map(TaskEntity::toDomain)
                .doOnSuccess(savedTask -> {
                    try {
                        publishDLQEvent(savedTask, error);
                    } catch (Exception e) {
                        log.error("Failed to publish DLQ event for task: {}", savedTask.taskId(), e);
                    }
                    try {
                        sendToDLQ(savedTask);
                    } catch (Exception e) {
                        log.error("Failed to send task to DLQ: {}", savedTask.taskId(), e);
                    }
                })
                .then();
    }

    private void sendToDLQ(Task task) {
        kafkaPublisher.send(dlqTopic, task.taskId(), task)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Task successfully moved to DLQ: {}", task.taskId());
                    } else {
                        log.error("Failed to move task to DLQ: {}, Error: {}",
                                task.taskId(), ex.getMessage(), ex);
                    }
                });
    }

    private void publishDLQEvent(final Task task, Throwable error) {
        final Map<String, Object> metadata = createErrorMetadata(task, error);

        final TaskEvent event = new TaskEvent(
                task.taskId(),
                task.type(),
                TaskEvent.EventTypes.MOVED_TO_DLQ,
                metadata,
                Instant.now()
        );

        eventPublisher.publishEvent(event);
    }

    private Map<String, Object> createErrorMetadata(final Task task, Throwable error) {
        final Map<String, Object> metadata = new HashMap<>();
        metadata.put("taskType", task.type());
        metadata.put("retryCount", task.retryCount());
        metadata.put("errorType", error.getClass().getName());
        metadata.put("errorMessage", error.getMessage());
        metadata.put("stackTrace", getStackTrace(error));
        metadata.put("timestamp", Instant.now().toString());
        return metadata;
    }

    private String getStackTrace(final Throwable error) {
        final StringWriter writer = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(writer)) {
            error.printStackTrace(printWriter);
        }
        return writer.toString();
    }
}