package cz.rb.task.controller;

import cz.rb.task.config.TaskExecutionEngineTestConfig;
import cz.rb.task.model.Task;
import cz.rb.task.model.TaskResult;
import cz.rb.task.model.TaskStatus;
import cz.rb.task.service.TaskAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for TaskAdminController.
 * Tests the HTTP layer and JSON serialization/deserialization.
 *
 * @Author: micfold
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = TaskExecutionEngineTestConfig.class
)
@ActiveProfiles("test")
class TaskAdminControllerIntegrationTest {

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private TaskAdminService taskAdminService;

    @Test
    @DisplayName("Should list tasks with advanced filtering")
    void listTasksWithFilters() {
        // Given
        final Task task1 = createSampleTask("task-1", "DOCUMENT_PROCESSING", TaskStatus.COMPLETED);
        final Task task2 = createSampleTask("task-2", "DOCUMENT_PROCESSING", TaskStatus.FAILED);

        when(taskAdminService.findTasks(
                eq("client-123"),
                eq(TaskStatus.COMPLETED),
                eq("DOCUMENT_PROCESSING"),
                any(Instant.class),
                any(Instant.class),
                eq(0),
                eq(10),
                eq("updatedAt"),
                eq("desc")
        )).thenReturn(Flux.just(task1));

        // Construct URI with all available filter params
        final Instant startDate = Instant.now().minus(1, ChronoUnit.DAYS);
        final Instant endDate = Instant.now();

        // When/Then
        webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/admin/tasks")
                        .queryParam("contextId", "client-123")
                        .queryParam("status", "COMPLETED")
                        .queryParam("type", "DOCUMENT_PROCESSING")
                        .queryParam("startDate", startDate.toString())
                        .queryParam("endDate", endDate.toString())
                        .queryParam("page", 0)
                        .queryParam("size", 10)
                        .queryParam("sortBy", "updatedAt")
                        .queryParam("sortDir", "desc")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Task.class)
                .hasSize(1)
                .contains(task1);

        verify(taskAdminService).findTasks(
                eq("client-123"),
                eq(TaskStatus.COMPLETED),
                eq("DOCUMENT_PROCESSING"),
                any(),
                any(),
                eq(0),
                eq(10),
                eq("updatedAt"),
                eq("desc")
        );
    }

    @Test
    @DisplayName("Should get task count statistics by type")
    void getTaskCounts() {
        // Given
        Map<TaskStatus, Long> statusCounts = Map.of(
                TaskStatus.PENDING, 5L,
                TaskStatus.IN_PROGRESS, 2L,
                TaskStatus.COMPLETED, 15L,
                TaskStatus.FAILED, 3L,
                TaskStatus.DEAD_LETTER, 1L
        );

        when(taskAdminService.countTasksByStatus("client-123", "EMAIL_NOTIFICATION"))
                .thenReturn(Mono.just(statusCounts));

        // When/Then
        webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/admin/tasks/count")
                        .queryParam("contextId", "client-123")
                        .queryParam("type", "EMAIL_NOTIFICATION")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.PENDING").isEqualTo(5)
                .jsonPath("$.IN_PROGRESS").isEqualTo(2)
                .jsonPath("$.COMPLETED").isEqualTo(15)
                .jsonPath("$.FAILED").isEqualTo(3)
                .jsonPath("$.DEAD_LETTER").isEqualTo(1);

        verify(taskAdminService).countTasksByStatus("client-123", "EMAIL_NOTIFICATION");
    }

    @Test
    @DisplayName("Should retry a failed task")
    void retryTask() {
        // Given
        String taskId = "failed-task-id";
        TaskResult.Success successResult = new TaskResult.Success(
                taskId, Map.of("status", "retried"));

        when(taskAdminService.retryTask(taskId)).thenReturn(Mono.just(successResult));

        // When/Then
        webClient.post()
                .uri("/api/v1/admin/tasks/{id}/retry", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.taskId").isEqualTo(taskId)
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("Task retry successful");

        verify(taskAdminService).retryTask(taskId);
    }

    @Test
    @DisplayName("Should handle not found when fetching task details")
    void getTaskDetails_notFound() {
        // Given
        String taskId = "non-existent";
        when(taskAdminService.getTaskById(taskId)).thenReturn(Mono.empty());

        // When/Then
        webClient.get()
                .uri("/api/v1/admin/tasks/{id}", taskId)
                .exchange()
                .expectStatus().isNotFound();

        verify(taskAdminService).getTaskById(taskId);
    }

    @Test
    @DisplayName("Should handle retry failure due to service error")
    void retryTask_serviceError() {
        // Given
        String taskId = "failed-task-id";
        when(taskAdminService.retryTask(taskId))
                .thenReturn(Mono.error(new IllegalStateException("Task not in FAILED status")));

        // When/Then
        webClient.post()
                .uri("/api/v1/admin/tasks/{id}/retry", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();

        verify(taskAdminService).retryTask(taskId);
    }

    private Task createSampleTask(String id, String type, TaskStatus status) {
        Instant now = Instant.now();
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