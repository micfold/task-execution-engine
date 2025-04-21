package cz.rb.task.controller;

import cz.rb.task.model.Task;
import cz.rb.task.model.TaskResult;
import cz.rb.task.model.TaskStatus;
import cz.rb.task.service.TaskAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for TaskAdminController
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 09.04.2025
 */
@ExtendWith(MockitoExtension.class)
class TaskAdminControllerTest {

    @Mock
    private TaskAdminService taskAdminService;

    @InjectMocks
    private TaskAdminController controller;

    private WebTestClient webClient;

    @Captor
    private ArgumentCaptor<String> idCaptor;

    @BeforeEach
    void setUp() {
        webClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    @DisplayName("Should return tasks when listing with filters")
    void listTasks() {
        // Given
        final Task task1 = createSampleTask("task-1", "type-1", TaskStatus.COMPLETED);
        final Task task2 = createSampleTask("task-2", "type-2", TaskStatus.PENDING);

        when(taskAdminService.findTasks(
                anyString(), any(), anyString(), any(), any(), anyInt(), anyInt(), anyString(), anyString()
        )).thenReturn(Flux.just(task1, task2));

        // When & Then
        webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/admin/tasks")
                        .queryParam("contextId", "client-123")
                        .queryParam("status", "PENDING")
                        .queryParam("type", "DOC_PROCESSING")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Task.class)
                .hasSize(2)
                .contains(task1, task2);

        verify(taskAdminService).findTasks(
                eq("client-123"),
                eq(TaskStatus.PENDING),
                eq("DOC_PROCESSING"),
                isNull(),
                isNull(),
                eq(0),
                eq(20),
                eq("createdAt"),
                eq("desc")
        );

    }

    @Test
    @DisplayName("Should return task details when task exists")
    void getTaskDetails_success() {
        // Given
        final String taskId = "task-123";
        final Task task = createSampleTask(taskId, "EMAIL_NOTIFICATION", TaskStatus.COMPLETED);
        when(taskAdminService.getTaskById(eq(taskId))).thenReturn(Mono.just(task));

        // When & Then
        webClient.get()
                .uri("/api/v1/admin/tasks/{id}", taskId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Task.class)
                .isEqualTo(task);

        verify(taskAdminService).getTaskById(taskId);
    }

    @Test
    @DisplayName("Should return 404 when task does not exist")
    void getTaskDetails_notFound() {
        // Given
        final String taskId = "non-existent";
        when(taskAdminService.getTaskById(eq(taskId))).thenReturn(Mono.empty());

        // When & Then
        webClient.get()
                .uri("/api/v1/admin/tasks/{id}", taskId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();

        verify(taskAdminService).getTaskById(taskId);
    }

    @Test
    @DisplayName("Should retry task successfully")
    void retryTask_success() {
        // Given
        final String taskId = "failed-task";
        final TaskResult.Success result = new TaskResult.Success(taskId, Map.of("status", "retried"));

        when(taskAdminService.retryTask(eq(taskId))).thenReturn(Mono.just(result));

        // When & Then
        webClient.post()
                .uri("/api/v1/admin/tasks/{id}/retry", taskId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.taskId").isEqualTo(taskId)
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("Task retry successful");

        verify(taskAdminService).retryTask(taskId);
    }

    @Test
    @DisplayName("Should handle retry failure")
    void retryTask_failure() {
        // Given
        final String taskId = "failed-task";
        final TaskResult.Failure result = new TaskResult.Failure(taskId, "Cannot retry task", false);

        when(taskAdminService.retryTask(eq(taskId))).thenReturn(Mono.just(result));

        // When & Then
        webClient.post()
                .uri("/api/v1/admin/tasks/{id}/retry", taskId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.taskId").isEqualTo(taskId)
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").isEqualTo("Cannot retry task");

        verify(taskAdminService).retryTask(taskId);
    }

    @Test
    @DisplayName("Should return tasks for specific user ID")
    void getTaskListByUserId() {
        // Given
        final String userId = "user-123";
        final Task task1 = createSampleTask("task-1", "EMAIL_NOTIFICATION", TaskStatus.COMPLETED);
        final Task task2 = createSampleTask("task-2", "DOCUMENT_PROCESSING", TaskStatus.PENDING);

        when(taskAdminService.getTasksByUserId(
                eq(userId),
                isNull(),
                eq(0),
                eq(20),
                eq("createdAt"),
                eq("desc")))
                .thenReturn(Flux.just(task1, task2));

        // When & Then
        webClient.get()
                .uri("/api/v1/admin/tasks/user/{userId}", userId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Task.class)
                .hasSize(2)
                .contains(task1, task2);

        verify(taskAdminService).getTasksByUserId(
                eq(userId),
                isNull(),
                eq(0),
                eq(20),
                eq("createdAt"),
                eq("desc"));
    }

    @Test
    @DisplayName("Should filter tasks by user ID and status")
    void getTaskListByUserId_withStatusFilter() {
        // Given
        final String userId = "user-123";
        final TaskStatus status = TaskStatus.COMPLETED;
        final Task task = createSampleTask("task-1", "EMAIL_NOTIFICATION", TaskStatus.COMPLETED);

        when(taskAdminService.getTasksByUserId(
                eq(userId),
                eq(status),
                eq(0),
                eq(20),
                eq("createdAt"),
                eq("desc")))
                .thenReturn(Flux.just(task));

        // When & Then
        webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/admin/tasks/user/{userId}")
                        .queryParam("status", "COMPLETED")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Task.class)
                .hasSize(1)
                .contains(task);

        verify(taskAdminService).getTasksByUserId(
                eq(userId),
                eq(status),
                eq(0),
                eq(20),
                eq("createdAt"),
                eq("desc"));
    }

    @Test
    @DisplayName("Should return empty list when no tasks found for user")
    void getTaskListByUserId_noTasks() {
        // Given
        final String userId = "user-without-tasks";

        when(taskAdminService.getTasksByUserId(
                eq(userId),
                isNull(),
                anyInt(),
                anyInt(),
                anyString(),
                anyString()))
                .thenReturn(Flux.empty());

        // When & Then
        webClient.get()
                .uri("/api/v1/admin/tasks/user/{userId}", userId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Task.class)
                .hasSize(0);

        verify(taskAdminService).getTasksByUserId(
                eq(userId),
                isNull(),
                eq(0),
                eq(20),
                eq("createdAt"),
                eq("desc"));
    }

    @Test
    @DisplayName("Should return task counts by status")
    void getTaskCounts() {
        // Given
        final Map<TaskStatus, Long> counts = Map.of(
                TaskStatus.PENDING, 5L,
                TaskStatus.IN_PROGRESS, 3L,
                TaskStatus.COMPLETED, 10L,
                TaskStatus.FAILED, 2L
        );

        // Using lenient() to fix the "strict stubbing argument mismatch" error
        // since we're checking for null contextId and specific type value
        when(taskAdminService.countTasksByStatus(isNull(), eq("EMAIL"))).thenReturn(Mono.just(counts));

        // When & Then
        webClient.get()
                .uri("/api/v1/admin/tasks/count?type=EMAIL")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.PENDING").isEqualTo(5)
                .jsonPath("$.IN_PROGRESS").isEqualTo(3)
                .jsonPath("$.COMPLETED").isEqualTo(10)
                .jsonPath("$.FAILED").isEqualTo(2);

        verify(taskAdminService).countTasksByStatus(isNull(), eq("EMAIL"));
    }

    private Task createSampleTask(final String id, final String type, final TaskStatus status) {
        final Instant now = Instant.now();
        return Task.builder()
                .taskId(id)
                .type(type)
                .status(status)
                .data(Map.of("clientId", "client-123", "documentId", "doc-456"))
                .retryCount(status == TaskStatus.FAILED ? 2 : 0)
                .createdAt(now.minus(30, ChronoUnit.MINUTES))
                .updatedAt(now)
                .build();
    }
}