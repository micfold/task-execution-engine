package cz.rb.task.engine;

import cz.rb.task.api.TaskHandler;
import cz.rb.task.messaging.DlqHandler;
import cz.rb.task.messaging.TaskEvent;
import cz.rb.task.messaging.TaskEventPublisher;
import cz.rb.task.model.Task;
import cz.rb.task.model.TaskResult;
import cz.rb.task.model.TaskStatus;
import cz.rb.task.persistence.TaskEntity;
import cz.rb.task.persistence.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultTaskExecutionEngineTest {

    @Mock
    private RetryStrategy retryStrategy;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private DlqHandler dlqHandler;

    @Mock
    private TaskEventPublisher eventPublisher;

    @Mock
    private TaskHandler taskHandler;

    private DefaultTaskExecutionEngine executionEngine;

    @BeforeEach
    void setUp() {
        executionEngine = new DefaultTaskExecutionEngine(
                retryStrategy,
                taskRepository,
                eventPublisher
        );

        // Default stubs for repository
        lenient().when(taskRepository.save(any()))
                .thenReturn(Mono.just(TaskEntity.fromDomain(createTestTask())));

        // Default stubs for event publisher
        lenient().doNothing().when(eventPublisher).publishEvent(any());
    }

    @Nested
    @DisplayName("Task Execution Tests")
    class TaskExecutionTests {

        @Test
        @DisplayName("Should successfully execute task and publish events")
        void successfulTaskExecution() {
            // Given
            var task = createTestTask();
            var taskEntity = TaskEntity.fromDomain(task);
            var successResult = new TaskResult.Success(
                    task.taskId(),
                    Map.of("result", "success")
            );

            when(taskRepository.save(any())).thenReturn(Mono.just(taskEntity));
            when(retryStrategy.executeWithRetry(eq(task), any())).thenReturn(Mono.just(successResult));

            // When
            var result = executionEngine.executeTask(task, taskHandler);

            // Then
            StepVerifier.create(result)
                    .consumeNextWith(taskResult -> {
                        assertThat(taskResult).isInstanceOf(TaskResult.Success.class);
                        var success = (TaskResult.Success) taskResult;
                        assertThat(success.result()).containsEntry("result", "success");
                    })
                    .verifyComplete();

            verify(taskRepository, times(2)).save(any());

            // Verify events
            ArgumentCaptor<TaskEvent> eventCaptor = ArgumentCaptor.forClass(TaskEvent.class);
            verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
            var events = eventCaptor.getAllValues();
            assertThat(events).hasSize(2);
            assertThat(events.get(0).eventType()).isEqualTo("TASK_STARTED");
            assertThat(events.get(1).eventType()).isEqualTo("TASK_COMPLETED");

            verifyNoInteractions(dlqHandler);
        }

        @ParameterizedTest(name = "Task failure handling - retryable: {0}")
        @MethodSource("provideFailureScenarios")
        @DisplayName("Should handle task failures appropriately")
        void taskFailureHandling(boolean isRetryable, String expectedEventType) {
            // Given
            var task = createTestTask();
            var taskEntity = TaskEntity.fromDomain(task);
            var failureResult = new TaskResult.Failure(
                    task.taskId(),
                    "Error occurred",
                    isRetryable
            );

            when(taskRepository.save(any())).thenReturn(Mono.just(taskEntity));
            when(retryStrategy.executeWithRetry(eq(task), any())).thenReturn(Mono.just(failureResult));

            // When
            var result = executionEngine.executeTask(task, taskHandler);

            // Then
            StepVerifier.create(result)
                    .consumeNextWith(taskResult -> {
                        assertThat(taskResult).isInstanceOf(TaskResult.Failure.class);
                        var failure = (TaskResult.Failure) taskResult;
                        assertThat(failure.error()).isEqualTo("Error occurred");
                    })
                    .verifyComplete();

            verify(taskRepository, times(2)).save(any());

            // Verify events
            ArgumentCaptor<TaskEvent> eventCaptor = ArgumentCaptor.forClass(TaskEvent.class);
            verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
            var events = eventCaptor.getAllValues();
            assertThat(events).hasSize(2);
            assertThat(events.get(0).eventType()).isEqualTo("TASK_STARTED");
            assertThat(events.get(1).eventType()).isEqualTo(expectedEventType);
        }

        private static Stream<Arguments> provideFailureScenarios() {
            return Stream.of(
                    Arguments.of(true, "TASK_FAILED"),
                    Arguments.of(false, "MOVED_TO_DLQ")
            );
        }
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("Should reject invalid task with missing required fields")
        void invalidTaskHandling() {
            // Given
            var invalidTask = Task.builder().build();

            // When/Then
            StepVerifier.create(executionEngine.executeTask(invalidTask, taskHandler))
                    .expectErrorSatisfies(error -> {
                        assertThat(error)
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Invalid task");
                    })
                    .verify();

            verifyNoInteractions(taskRepository, eventPublisher, dlqHandler);
        }

        @Test
        @DisplayName("Should handle null task handler")
        void nullTaskHandlerHandling() {
            var task = createTestTask();

            StepVerifier.create(executionEngine.executeTask(task, null))
                    .expectErrorSatisfies(error -> {
                        assertThat(error)
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Task handler cannot be null");
                    })
                    .verify();

            verifyNoInteractions(taskRepository, eventPublisher, dlqHandler);
        }
    }

    private Task createTestTask() {
        return Task.builder()
                .taskId(UUID.randomUUID().toString())
                .type("TEST_TASK")
                .status(TaskStatus.PENDING)
                .data(Map.of())
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}