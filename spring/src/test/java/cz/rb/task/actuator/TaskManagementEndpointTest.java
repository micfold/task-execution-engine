package cz.rb.task.actuator;

import cz.rb.task.model.Task;
import cz.rb.task.model.TaskStatus;
import cz.rb.task.service.TaskAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskManagementEndpointTest {

    @Mock
    private TaskAdminService taskAdminService;

    @InjectMocks
    private TaskManagementEndpoint endpoint;

    @Test
    @DisplayName("Should return task details with execution time for completed task")
    void getTaskDetails_completed() {
        // Given
        String taskId = "completed-task";
        Instant createdAt = Instant.now().minus(1, ChronoUnit.MINUTES);
        Instant updatedAt = Instant.now();
        long executionTimeMs = updatedAt.toEpochMilli() - createdAt.toEpochMilli();

        Task task = Task.builder()
                .taskId(taskId)
                .type("DOCUMENT_PROCESSING")
                .status(TaskStatus.COMPLETED)
                .data(Map.of("documentId", "doc-123"))
                .retryCount(0)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        when(taskAdminService.getTaskById(taskId)).thenReturn(Mono.just(task));

        // When
        Mono<Map<String, Object>> result = endpoint.getTaskDetails(taskId);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(details -> {
                    assertThat(details.get("id")).isEqualTo(taskId);
                    assertThat(details.get("type")).isEqualTo("DOCUMENT_PROCESSING");
                    assertThat(details.get("status")).isEqualTo(TaskStatus.COMPLETED);
                    assertThat(details.get("executionTimeMs")).isNotNull();
                    long returnedTime = (long) details.get("executionTimeMs");
                    // Allow small difference due to test execution time
                    assertThat(returnedTime).isCloseTo(executionTimeMs, within(100L));
                    assertThat(details.get("executionTimeFormatted")).isNotNull();
                    return true;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return task details without execution time for non-completed task")
    void getTaskDetails_pending() {
        // Given
        String taskId = "pending-task";
        Instant now = Instant.now();

        Task task = Task.builder()
                .taskId(taskId)
                .type("DOCUMENT_PROCESSING")
                .status(TaskStatus.PENDING)
                .data(Map.of("documentId", "doc-123"))
                .retryCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(taskAdminService.getTaskById(taskId)).thenReturn(Mono.just(task));

        // When
        Mono<Map<String, Object>> result = endpoint.getTaskDetails(taskId);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(details -> {
                    assertThat(details.get("id")).isEqualTo(taskId);
                    assertThat(details.get("type")).isEqualTo("DOCUMENT_PROCESSING");
                    assertThat(details.get("status")).isEqualTo(TaskStatus.PENDING);
                    // Should not have execution time for non-completed tasks
                    assertThat(details.containsKey("executionTimeMs")).isFalse();
                    assertThat(details.containsKey("executionTimeFormatted")).isFalse();
                    return true;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return task list for specific user ID")
    void getTaskListByUserId_success() {
        // Given
        final String userId = "user-123";
        final Instant createdAt = Instant.now().minus(30, ChronoUnit.MINUTES);
        final Instant updatedAt = Instant.now().minus(5, ChronoUnit.MINUTES);

        final Task task1 = Task.builder()
                .taskId("task-1")
                .type("DOCUMENT_PROCESSING")
                .status(TaskStatus.COMPLETED)
                .data(Map.of("userId", userId, "documentId", "doc-123"))
                .retryCount(0)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        final Task task2 = Task.builder()
                .taskId("task-2")
                .type("EMAIL_NOTIFICATION")
                .status(TaskStatus.PENDING)
                .data(Map.of("userId", userId, "email", "user@example.com"))
                .retryCount(0)
                .createdAt(createdAt)
                .updatedAt(createdAt) // Same as created for pending
                .build();

        when(taskAdminService.getTasksByUserId(eq(userId), isNull(), eq(0), eq(100), eq("createdAt"), eq("desc")))
                .thenReturn(Flux.just(task1, task2));

        // When
        final Flux<Map<String, Object>> result = endpoint.getTaskListByUserId(userId);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(details -> {
                    assertThat(details.get("id")).isEqualTo("task-1");
                    assertThat(details.get("type")).isEqualTo("DOCUMENT_PROCESSING");
                    assertThat(details.get("status")).isEqualTo(TaskStatus.COMPLETED);
                    // Should have execution time for completed task
                    assertThat(details.containsKey("executionTimeMs")).isTrue();
                    assertThat(details.containsKey("executionTimeFormatted")).isTrue();
                    return true;
                })
                .expectNextMatches(details -> {
                    assertThat(details.get("id")).isEqualTo("task-2");
                    assertThat(details.get("type")).isEqualTo("EMAIL_NOTIFICATION");
                    assertThat(details.get("status")).isEqualTo(TaskStatus.PENDING);
                    // Should not have execution time for pending tasks
                    assertThat(details.containsKey("executionTimeMs")).isFalse();
                    assertThat(details.containsKey("executionTimeFormatted")).isFalse();
                    return true;
                })
                .verifyComplete();

        verify(taskAdminService).getTasksByUserId(eq(userId), isNull(), eq(0), eq(100), eq("createdAt"), eq("desc"));
    }

    @Test
    @DisplayName("Should return empty result when no tasks found for user")
    void getTaskListByUserId_noTasks() {
        // Given
        final String userId = "user-without-tasks";

        when(taskAdminService.getTasksByUserId(eq(userId), isNull(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(Flux.empty());

        // When
        final Flux<Map<String, Object>> result = endpoint.getTaskListByUserId(userId);

        // Then
        StepVerifier.create(result)
                .expectComplete()
                .verify();

        verify(taskAdminService).getTasksByUserId(eq(userId), isNull(), eq(0), eq(100), eq("createdAt"), eq("desc"));
    }

    @Test
    @DisplayName("Should mark task as failed successfully")
    void markTaskFailed_success() {
        // Given
        String taskId = "in-progress-task";

        Task task = Task.builder()
                .taskId(taskId)
                .type("DOCUMENT_PROCESSING")
                .status(TaskStatus.IN_PROGRESS)
                .data(Map.of("documentId", "doc-123"))
                .retryCount(0)
                .createdAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .updatedAt(Instant.now().minus(2, ChronoUnit.MINUTES))
                .build();

        Task failedTask = task.withStatus(TaskStatus.FAILED);

        when(taskAdminService.getTaskById(taskId)).thenReturn(Mono.just(task));
        when(taskAdminService.updateTask(any())).thenReturn(Mono.just(failedTask));

        // When
        Mono<Map<String, Object>> result = endpoint.markTaskFailed(taskId);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertThat(response.get("success")).isEqualTo(true);
                    assertThat(response.get("message")).isEqualTo("Task marked as failed");
                    assertThat(response.get("taskId")).isEqualTo(taskId);
                    return true;
                })
                .verifyComplete();

        verify(taskAdminService).updateTask(argThat(t ->
                t.taskId().equals(taskId) &&
                        t.status() == TaskStatus.FAILED));
    }

    @Test
    @DisplayName("Should not mark completed task as failed")
    void markTaskFailed_completedTask() {
        // Given
        String taskId = "completed-task";

        Task task = Task.builder()
                .taskId(taskId)
                .type("DOCUMENT_PROCESSING")
                .status(TaskStatus.COMPLETED)
                .data(Map.of("documentId", "doc-123"))
                .retryCount(0)
                .createdAt(Instant.now().minus(10, ChronoUnit.MINUTES))
                .updatedAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .build();

        when(taskAdminService.getTaskById(taskId)).thenReturn(Mono.just(task));

        // When
        Mono<Map<String, Object>> result = endpoint.markTaskFailed(taskId);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertThat(response.get("success")).isEqualTo(false);
                    assertThat((String)response.get("message")).contains("Can only mark IN_PROGRESS or PENDING tasks as failed");
                    return true;
                })
                .verifyComplete();

        verify(taskAdminService, never()).updateTask(any());
    }

    @Test
    @DisplayName("Should handle task not found when marking as failed")
    void markTaskFailed_taskNotFound() {
        // Given
        String taskId = "non-existent";
        when(taskAdminService.getTaskById(taskId)).thenReturn(Mono.empty());

        // When
        Mono<Map<String, Object>> result = endpoint.markTaskFailed(taskId);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertThat(response.get("success")).isEqualTo(false);
                    assertThat((String)response.get("message")).contains("Task not found");
                    return true;
                })
                .verifyComplete();
    }
}