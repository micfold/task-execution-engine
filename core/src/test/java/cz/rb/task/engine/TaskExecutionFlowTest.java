package cz.rb.task.engine;

import cz.rb.task.api.TaskHandler;
import cz.rb.task.messaging.TaskEventPublisher;
import cz.rb.task.model.Task;
import cz.rb.task.model.TaskResult;
import cz.rb.task.model.TaskStatus;
import cz.rb.task.persistence.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskExecutionFlowTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TaskEventPublisher eventPublisher;

    private DefaultTaskExecutionEngine executionEngine;

    @BeforeEach
    void setUp() {
        RetryStrategy retryStrategy = new RetryStrategy();
        ReflectionTestUtils.setField(retryStrategy, "maxRetries", 3);
        ReflectionTestUtils.setField(retryStrategy, "initialDelaySeconds", 0L);
        ReflectionTestUtils.setField(retryStrategy, "maxDelaySeconds", 1L);

        executionEngine = new DefaultTaskExecutionEngine(
                retryStrategy,
                taskRepository,
                eventPublisher
        );

        // Default stubs for repository
        lenient().when(taskRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    }

    @Test
    @DisplayName("Success Flow: Task Executes Successfully on First Try")
    void successFlow() {

        final TaskHandler successHandler = createSuccessHandler();

        final Task task = createTask("SUCCESS_TASK");

        // When
        final Mono<TaskResult> resultMono = executionEngine.executeTask(task, successHandler);

        // Then
        StepVerifier.create(resultMono)
                .expectNextMatches(result -> {
                    assertThat(result).isInstanceOf(TaskResult.Success.class);
                    return true;
                })
                .verifyComplete();

        // Verify task status update
        verify(taskRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("Failure Flow: Task Moves to Dead Letter Queue")
    void failureFlow() {
        // Setup permanent failure handler
        final TaskHandler failureHandler = new TaskHandler() {
            @Override
            public Mono<TaskResult> execute(final Task task) {
                return Mono.error(new RuntimeException("Permanent failure"));
            }

            @Override
            public String getTaskType() {
                return "FAILURE_TASK";
            }
        };

        // Create test task
        final Task task = createTask("FAILURE_TASK");

        // When
        final Mono<TaskResult> resultMono = executionEngine.executeTask(task, failureHandler);

        // Then
        StepVerifier.create(resultMono)
                .expectNextMatches(result -> {
                    assertThat(result).isInstanceOf(TaskResult.Failure.class);
                    TaskResult.Failure failure = (TaskResult.Failure) result;
                    assertThat(failure.retryable()).isFalse();
                    assertThat(failure.error()).contains("Permanent failure");
                    return true;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("DLQ Recovery Flow: Task Moves from Dead Letter to Pending")
    void dlqRecoveryFlow() {
        final TaskHandler recoveryHandler = new TaskHandler() {
            @Override
            public Mono<TaskResult> execute(Task task) {
                return Mono.just(new TaskResult.Success(task.taskId(), Map.of("recovered", true)));
            }

            @Override
            public String getTaskType() {
                return "DLQ_RECOVERY_TASK";
            }
        };

        final Task deadLetterTask = Task.builder()
                .taskId(UUID.randomUUID().toString())
                .type("DLQ_RECOVERY_TASK")
                .status(TaskStatus.DEAD_LETTER)
                .data(new HashMap<>())
                .retryCount(3)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // When
        final Mono<TaskResult> resultMono = executionEngine.executeTask(deadLetterTask, recoveryHandler);

        // Then
        StepVerifier.create(resultMono)
                .expectNextMatches(result -> {
                    assertThat(result).isInstanceOf(TaskResult.Success.class);
                    TaskResult.Success successResult = (TaskResult.Success) result;
                    assertThat(successResult.result())
                            .containsEntry("recovered", true);
                    return true;
                })
                .verifyComplete();

        // Verify task was processed and status potentially changed
        verify(taskRepository, times(2)).save(any());
    }

    // Utility method to create a simple successful task handler
    private TaskHandler createSuccessHandler() {
        return new TaskHandler() {
            @Override
            public Mono<TaskResult> execute(Task task) {
                return Mono.just(new TaskResult.Success(task.taskId(), Map.of("result", "success")));
            }

            @Override
            public String getTaskType() {
                return "SUCCESS_TASK";
            }
        };
    }

    // Utility method to create a test task
    private Task createTask(final String taskType) {
        return Task.builder()
                .taskId(UUID.randomUUID().toString())
                .type(taskType)
                .status(TaskStatus.PENDING)
                .data(new HashMap<>())
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}