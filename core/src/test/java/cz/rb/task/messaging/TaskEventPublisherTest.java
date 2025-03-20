package cz.rb.task.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * This is the default java doc for the class
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 20.03.2025
 */
@ExtendWith(MockitoExtension.class)
class TaskEventPublisherTest {

    @Mock
    private KafkaTemplate<String, TaskEvent> kafkaTemplate;

    @Captor
    private ArgumentCaptor<TaskEvent> eventCaptor;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    private TaskEventPublisher eventPublisher;
    private static final String TEST_TOPIC = "test-task-events";

    @BeforeEach
    void setUp() {
        eventPublisher = new TaskEventPublisher(kafkaTemplate);
        ReflectionTestUtils.setField(eventPublisher, "taskEventsTopic", TEST_TOPIC);
    }

    @Nested
    @DisplayName("Event Publishing Tests")
    class EventPublishingTests {

        @Test
        @DisplayName("Should publish task event successfully")
        void publishEventSuccess() {
            // Given
            String taskId = UUID.randomUUID().toString();
            TaskEvent event = createTestEvent(taskId);

            SendResult<String, TaskEvent> sendResult = mock(SendResult.class);
            when(kafkaTemplate.send(any(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(sendResult));

            // When
            eventPublisher.publishEvent(event);

            // Then
            verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

            String capturedTopic = topicCaptor.getValue();
            String capturedKey = keyCaptor.getValue();
            TaskEvent capturedEvent = eventCaptor.getValue();

            assertThat(capturedTopic).isEqualTo(TEST_TOPIC);
            assertThat(capturedKey).isEqualTo(taskId);
            assertThat(capturedEvent).isEqualTo(event);
        }

        @Test
        @DisplayName("Should handle Kafka publish failure gracefully")
        void publishEventFailure() {
            // Given
            String taskId = UUID.randomUUID().toString();
            TaskEvent event = createTestEvent(taskId);

            RuntimeException kafkaException = new RuntimeException("Kafka error");
            when(kafkaTemplate.send(any(), any(), any()))
                    .thenReturn(CompletableFuture.failedFuture(kafkaException));

            // When - should not throw exception
            eventPublisher.publishEvent(event);

            // Then
            verify(kafkaTemplate).send(eq(TEST_TOPIC), eq(taskId), eq(event));
            // No exceptions thrown, errors are logged
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create task started event correctly")
        void taskStartedEvent() {
            // Given
            String taskId = UUID.randomUUID().toString();
            String taskType = "TEST_TASK";

            // When
            TaskEvent event = TaskEvent.taskStarted(taskId, taskType);

            // Then
            assertThat(event.taskId()).isEqualTo(taskId);
            assertThat(event.taskType()).isEqualTo(taskType);
            assertThat(event.eventType()).isEqualTo(TaskEvent.EventTypes.TASK_STARTED);
            assertThat(event.metadata()).isEmpty();
            assertThat(event.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should create task completed event correctly")
        void taskCompletedEvent() {
            // Given
            String taskId = UUID.randomUUID().toString();
            String taskType = "TEST_TASK";
            Map<String, Object> result = Map.of("key", "value");

            // When
            TaskEvent event = TaskEvent.taskCompleted(taskId, taskType, result);

            // Then
            assertThat(event.taskId()).isEqualTo(taskId);
            assertThat(event.taskType()).isEqualTo(taskType);
            assertThat(event.eventType()).isEqualTo(TaskEvent.EventTypes.TASK_COMPLETED);
            assertThat(event.metadata()).isEqualTo(result);
            assertThat(event.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should create task failed event correctly")
        void taskFailedEvent() {
            // Given
            String taskId = UUID.randomUUID().toString();
            String taskType = "TEST_TASK";
            String error = "Test error";
            boolean retryable = true;

            // When
            TaskEvent event = TaskEvent.taskFailed(taskId, taskType, error, retryable);

            // Then
            assertThat(event.taskId()).isEqualTo(taskId);
            assertThat(event.taskType()).isEqualTo(taskType);
            assertThat(event.eventType()).isEqualTo(TaskEvent.EventTypes.TASK_FAILED);
            assertThat(event.metadata())
                    .containsEntry("error", error)
                    .containsEntry("retryable", retryable);
            assertThat(event.timestamp()).isNotNull();
        }
    }

    private TaskEvent createTestEvent(String taskId) {
        return new TaskEvent(
                taskId,
                "TEST_TASK",
                TaskEvent.EventTypes.TASK_COMPLETED,
                Map.of("test", "data"),
                Instant.now()
        );
    }
}