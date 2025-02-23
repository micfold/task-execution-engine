package cz.rb.task.engine;

import cz.rb.task.exception.RetryableException;
import cz.rb.task.model.Task;
import cz.rb.task.model.TaskResult;
import cz.rb.task.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryStrategyTest {

    private RetryStrategy retryStrategy;
    private static final int MAX_RETRIES = 3;

    @BeforeEach
    void setUp() {
        retryStrategy = new RetryStrategy();
        ReflectionTestUtils.setField(retryStrategy, "maxRetries", MAX_RETRIES);
        ReflectionTestUtils.setField(retryStrategy, "initialDelaySeconds", 0L);
        ReflectionTestUtils.setField(retryStrategy, "maxDelaySeconds", 1L);
    }

    @Test
    void executeWithRetry_SuccessFirstTry() {
        Task task = createTestTask();
        TaskResult expectedResult = new TaskResult.Success(task.taskId(), null);

        StepVerifier.create(retryStrategy.executeWithRetry(task, t -> Mono.just(expectedResult)))
                .expectNext(expectedResult)
                .verifyComplete();
    }

    @Test
    void executeWithRetry_SuccessAfterRetries() {
        Task task = createTestTask();
        AtomicInteger attempts = new AtomicInteger(0);
        TaskResult expectedResult = new TaskResult.Success(task.taskId(), null);

        StepVerifier.create(
                        retryStrategy.executeWithRetry(task, t -> {
                            if (attempts.incrementAndGet() <= 2) { // First two attempts fail
                                return Mono.error(new RetryableException("Temporary error"));
                            }
                            return Mono.just(expectedResult); // Third attempt succeeds
                        })
                )
                .expectSubscription()
                .expectNext(expectedResult)
                .verifyComplete();

        assertEquals(3, attempts.get(), "Should succeed on third attempt");
    }

    @Test
    void executeWithRetry_NonRetryableError() {
        Task task = createTestTask();
        AtomicInteger attempts = new AtomicInteger(0);

        StepVerifier.create(
                        retryStrategy.executeWithRetry(task, t -> {
                            attempts.incrementAndGet();
                            return Mono.error(new RuntimeException("Non-retryable error"));
                        })
                )
                .expectNextMatches(result ->
                        result instanceof TaskResult.Failure &&
                                ((TaskResult.Failure) result).error().contains("Non-retryable error") &&
                                !((TaskResult.Failure) result).retryable()
                )
                .verifyComplete();

        assertEquals(1, attempts.get(), "Should not retry for non-retryable error");
    }

    @Test
    void executeWithRetry_NestedRetryableException() {
        final Task task = createTestTask();
        final AtomicInteger attempts = new AtomicInteger(0);
        final Exception nestedError = new RuntimeException(
                "Outer error",
                new RetryableException("Inner retryable error")
        );

        StepVerifier.create(
                        retryStrategy.executeWithRetry(task, t -> {
                            attempts.incrementAndGet();
                            return Mono.error(nestedError);
                        })
                )
                .expectNextMatches(result ->
                        result instanceof TaskResult.Failure &&
                                ((TaskResult.Failure) result).retryable() &&
                                attempts.get() == MAX_RETRIES + 1
                )
                .verifyComplete();
    }

    @Test
    void executeWithRetry_NullTask() {
        StepVerifier.create(retryStrategy.executeWithRetry(null, t -> Mono.empty()))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void executeWithRetry_NullExecution() {
        StepVerifier.create(retryStrategy.executeWithRetry(createTestTask(), null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    private Task createTestTask() {
        return Task.builder()
                .taskId(UUID.randomUUID().toString())
                .type("TEST_TASK")
                .status(TaskStatus.PENDING)
                .retryCount(0)
                .build();
    }
}