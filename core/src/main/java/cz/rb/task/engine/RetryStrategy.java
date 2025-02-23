package cz.rb.task.engine;

import cz.rb.task.model.Task;
import cz.rb.task.model.TaskResult;
import cz.rb.task.exception.RetryableException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Setter
@Getter
@Slf4j
@Component
public class RetryStrategy {

    @Value("${task.execution.max-retries:3}")
    private int maxRetries;

    @Value("${task.execution.initial-delay:1}")
    private long initialDelaySeconds;

    @Value("${task.execution.max-delay:60}")
    private long maxDelaySeconds;

    public Mono<TaskResult> executeWithRetry(final Task task,
                                             Function<Task, Mono<TaskResult>> execution) {
        if (task == null) {
            return Mono.error(new IllegalArgumentException("Task cannot be null"));
        }
        if (execution == null) {
            return Mono.error(new IllegalArgumentException("Execution function cannot be null"));
        }

        final AtomicInteger attemptCount = new AtomicInteger(0);

        return Mono.defer(() -> {
                    final int currentAttempt = attemptCount.incrementAndGet();
                    log.debug("Executing task: {}, Attempt: {}", task.taskId(), currentAttempt);
                    return execution.apply(task);
                })
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(initialDelaySeconds))
                        .maxBackoff(Duration.ofSeconds(maxDelaySeconds))
                        .filter(this::isRetryableException)
                        .doBeforeRetry(signal -> {
                            final int attempt = attemptCount.get();
                            log.info("Retrying task: {}, Attempt: {}/{}",
                                    task.taskId(), attempt, maxRetries);
                        }))
                .onErrorResume(error -> {
                    log.error("Task execution failed. Task ID: {}, Error: {}",
                            task.taskId(), error.getMessage(), error);

                    final boolean isRetryable = isRetryableException(error);
                    return Mono.just(new TaskResult.Failure(
                            task.taskId(),
                            String.format("Execution failed after %d attempts: %s",
                                    attemptCount.get(), error.getMessage()),
                            isRetryable
                    ));
                });
    }

    private boolean isRetryableException(final Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof RetryableException ||
                    current instanceof TimeoutException ||
                    current instanceof TransientDataAccessException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}