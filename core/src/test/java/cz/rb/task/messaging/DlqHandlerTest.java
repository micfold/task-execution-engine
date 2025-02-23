package cz.rb.task.messaging;

import cz.rb.task.model.Task;
import cz.rb.task.model.TaskStatus;
import cz.rb.task.persistence.TaskEntity;
import cz.rb.task.persistence.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DlqHandlerTest {

    @Mock private TaskRepository taskRepository;
    @Mock private KafkaPublisher<String, Task> kafkaPublisher;
    @Mock private TaskEventPublisher eventPublisher;

    @Captor private ArgumentCaptor<TaskEntity> taskEntityCaptor;
    @Captor private ArgumentCaptor<TaskEvent> eventCaptor;

    private DlqHandler dlqHandler;
    private static final String DLQ_TOPIC = "test.dlq.topic";

    record TestError(String message, Throwable cause) {
        static TestError of(String message) {
            return new TestError(message, new RuntimeException(message));
        }
    }

    @BeforeEach
    void setUp() {
        dlqHandler = new DlqHandler(taskRepository, kafkaPublisher, eventPublisher);
        ReflectionTestUtils.setField(dlqHandler, "dlqTopic", DLQ_TOPIC);
    }

    @Nested
    @DisplayName("Dead Letter Handling")
    class DeadLetterHandlingTests {

        @Test
        @DisplayName("Should successfully handle dead letter and publish event")
        void successfulDeadLetterHandling() {
            // Given
            var task = createTestTask();
            var taskEntity = TaskEntity.fromDomain(task.withStatus(TaskStatus.DEAD_LETTER));
            var error = TestError.of("Test error");

            when(taskRepository.save(any())).thenReturn(Mono.just(taskEntity));
            when(kafkaPublisher.send(any(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
            doNothing().when(eventPublisher).publishEvent(any());

            // When
            var result = dlqHandler.handleDeadLetter(task, error.cause());

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(taskRepository).save(taskEntityCaptor.capture());
            var savedEntity = taskEntityCaptor.getValue();
            assertThat(savedEntity)
                    .satisfies(entity -> {
                        assertThat(entity.status()).isEqualTo(TaskStatus.DEAD_LETTER);
                        assertThat(entity.taskId()).isEqualTo(task.taskId());
                    });

            verify(kafkaPublisher).send(
                    eq(DLQ_TOPIC),
                    eq(task.taskId()),
                    any(Task.class)
            );

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            var publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent)
                    .satisfies(event -> {
                        assertThat(event.eventType()).isEqualTo(TaskEvent.EventTypes.MOVED_TO_DLQ);
                        assertThat(event.taskId()).isEqualTo(task.taskId());
                        assertThat(event.metadata())
                                .containsKey("errorMessage")
                                .containsKey("stackTrace");
                    });
        }

        @Test
        @DisplayName("Should handle repository errors gracefully")
        void repositoryErrorHandling() {
            // Given
            var task = createTestTask();
            var error = TestError.of("Repository error");

            when(taskRepository.save(any())).thenReturn(Mono.error(error.cause()));

            // When
            var result = dlqHandler.handleDeadLetter(task, new RuntimeException("Original error"));

            // Then
            StepVerifier.create(result)
                    .verifyErrorSatisfies(throwable ->
                            assertThat(throwable)
                                    .isInstanceOf(RuntimeException.class)
                                    .hasMessage(error.message()));

            verify(kafkaPublisher, never()).send(any(), any(), any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should continue processing despite Kafka errors")
        void kafkaErrorHandling() {
            // Given
            var task = createTestTask();
            var taskEntity = TaskEntity.fromDomain(task.withStatus(TaskStatus.DEAD_LETTER));
            var error = TestError.of("Kafka error");

            when(taskRepository.save(any())).thenReturn(Mono.just(taskEntity));
            when(kafkaPublisher.send(any(), any(), any()))
                    .thenReturn(CompletableFuture.failedFuture(error.cause()));
            doNothing().when(eventPublisher).publishEvent(any());

            // When
            var result = dlqHandler.handleDeadLetter(task, new RuntimeException("Original error"));

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(taskRepository).save(any());
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("Should continue processing despite event publishing errors")
        void eventPublishingErrorHandling() {
            // Given
            var task = createTestTask();
            var taskEntity = TaskEntity.fromDomain(task.withStatus(TaskStatus.DEAD_LETTER));
            var error = TestError.of("Event publishing error");

            when(taskRepository.save(any())).thenReturn(Mono.just(taskEntity));
            when(kafkaPublisher.send(any(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
            doThrow(error.cause()).when(eventPublisher).publishEvent(any());

            // When
            var result = dlqHandler.handleDeadLetter(task, new RuntimeException("Original error"));

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(taskRepository).save(any());
            verify(kafkaPublisher).send(any(), any(), any());
        }
    }

    private Task createTestTask() {
        return Task.builder()
                .taskId(UUID.randomUUID().toString())
                .type("TEST_TASK")
                .status(TaskStatus.FAILED)
                .data(Map.of())
                .retryCount(3)
                .build();
    }
}