package cz.rb.task.service;

import cz.rb.task.api.TaskHandler;
import cz.rb.task.engine.DefaultTaskExecutionEngine;
import cz.rb.task.engine.TaskHandlerRegistry;
import cz.rb.task.exception.TaskNotFoundException;
import cz.rb.task.model.Task;
import cz.rb.task.model.TaskResult;
import cz.rb.task.model.TaskStatus;
import cz.rb.task.persistence.TaskEntity;
import cz.rb.task.persistence.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * This is the default java doc for the class
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 09.04.2025
 */
@ExtendWith(MockitoExtension.class)
class TaskAdminServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskHandlerRegistry taskHandlerRegistry;

    @Mock
    private DefaultTaskExecutionEngine taskExecutionEngine;

    @Mock
    private TaskHandler taskHandler;

    @Captor
    private ArgumentCaptor<TaskEntity> taskEntityCaptor;

    @Captor
    private ArgumentCaptor<PageRequest> pageRequestCaptor;

    private TaskAdminService taskAdminService;

    @BeforeEach
    void setUp() {
        taskAdminService = new TaskAdminService(taskRepository, taskHandlerRegistry, taskExecutionEngine);
    }

    @Test
    @DisplayName("Should find tasks with type and status filters")
    void findTasks_withTypeAndStatus() {
        // Given
        final Task task1 = createSampleTask("task-1", "EMAIL", TaskStatus.PENDING);
        final Task task2 = createSampleTask("task-2", "EMAIL", TaskStatus.PENDING);
        final TaskEntity entity1 = TaskEntity.fromDomain(task1);
        final TaskEntity entity2 = TaskEntity.fromDomain(task2);

        when(taskRepository.findByTypeAndStatus(
                eq("EMAIL"), eq(TaskStatus.PENDING), any(PageRequest.class))
        ).thenReturn(Flux.just(entity1, entity2));

        // When
        final Flux<Task> result = taskAdminService.findTasks(
                null, TaskStatus.PENDING, "EMAIL", null, null, 0, 10, "createdAt", "desc");

        // Then
        StepVerifier.create(result)
                .expectNext(task1, task2)
                .verifyComplete();

        verify(taskRepository).findByTypeAndStatus(
                eq("EMAIL"), eq(TaskStatus.PENDING), pageRequestCaptor.capture());

        final PageRequest capturedRequest = pageRequestCaptor.getValue();
        assertThat(capturedRequest.getPageNumber()).isEqualTo(0);
        assertThat(capturedRequest.getPageSize()).isEqualTo(10);
        assertThat(capturedRequest.getSort().getOrderFor("created_at").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("Should find tasks with status filter only")
    void findTasks_withStatusOnly() {
        // Given
        final Task task1 = createSampleTask("task-1", "EMAIL", TaskStatus.COMPLETED);
        final Task task2 = createSampleTask("task-2", "DOCUMENT", TaskStatus.COMPLETED);
        final TaskEntity entity1 = TaskEntity.fromDomain(task1);
        final TaskEntity entity2 = TaskEntity.fromDomain(task2);

        when(taskRepository.findByStatus(
                eq(TaskStatus.COMPLETED), any(PageRequest.class))
        ).thenReturn(Flux.just(entity1, entity2));

        // When
        final Flux<Task> result = taskAdminService.findTasks(
                null, TaskStatus.COMPLETED, null, null, null, 0, 10, "updatedAt", "asc");

        // Then
        StepVerifier.create(result)
                .expectNext(task1, task2)
                .verifyComplete();

        verify(taskRepository).findByStatus(
                eq(TaskStatus.COMPLETED), pageRequestCaptor.capture());

        final PageRequest capturedRequest = pageRequestCaptor.getValue();
        assertThat(capturedRequest.getSort().getOrderFor("updated_at").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("Should find tasks with context ID filter")
    void findTasks_withContextId() {
        // Given
        final Task task1 = createSampleTask("task-1", "EMAIL", TaskStatus.PENDING,
                Map.of("clientId", "client-123"));
        final Task task2 = createSampleTask("task-2", "EMAIL", TaskStatus.PENDING,
                Map.of("clientId", "client-456")); // Different client ID

        final TaskEntity entity1 = TaskEntity.fromDomain(task1);
        final TaskEntity entity2 = TaskEntity.fromDomain(task2);

        when(taskRepository.findAll()).thenReturn(Flux.just(entity1, entity2));

        // When
        final Flux<Task> result = taskAdminService.findTasks(
                "client-123", null, null, null, null, 0, 10, "createdAt", "desc");

        // Then
        StepVerifier.create(result)
                .expectNext(task1) // Only task1 matches the clientId
                .verifyComplete();
    }

    @Test
    @DisplayName("Should get task by ID")
    void getTaskById_success() {
        // Given
        final String taskId = "task-123";
        final Task task = createSampleTask(taskId, "EMAIL", TaskStatus.COMPLETED);
        final TaskEntity entity = TaskEntity.fromDomain(task);

        when(taskRepository.findById(taskId)).thenReturn(Mono.just(entity));

        // When
        final Mono<Task> result = taskAdminService.getTaskById(taskId);

        // Then
        StepVerifier.create(result)
                .expectNext(task)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should throw exception when task not found")
    void getTaskById_notFound() {
        // Given
        final String taskId = "non-existent";
        when(taskRepository.findById(taskId)).thenReturn(Mono.empty());

        // When
        final Mono<Task> result = taskAdminService.getTaskById(taskId);

        // Then
        StepVerifier.create(result)
                .expectError(TaskNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("Should retry a failed task")
    void retryTask_success() {
        // Given
        final String taskId = "failed-task";
        final Task task = createSampleTask(taskId, "EMAIL", TaskStatus.FAILED, Map.of(), 2);
        final TaskEntity entity = TaskEntity.fromDomain(task);
        final TaskResult.Success resultSuccess = new TaskResult.Success(taskId, Map.of("status", "retried"));

        when(taskRepository.findById(taskId)).thenReturn(Mono.just(entity));
        when(taskHandlerRegistry.getHandler("EMAIL")).thenReturn(Mono.just(taskHandler));
        when(taskExecutionEngine.executeTask(any(), eq(taskHandler))).thenReturn(Mono.just(resultSuccess));

        // When
        final Mono<TaskResult> result = taskAdminService.retryTask(taskId);

        // Then
        StepVerifier.create(result)
                .expectNext(resultSuccess)
                .verifyComplete();

        verify(taskExecutionEngine).executeTask(argThat(t ->
                        t.taskId().equals(taskId) &&
                                t.status() == TaskStatus.PENDING &&
                                t.type().equals("EMAIL")),
                eq(taskHandler));
    }

    @Test
    @DisplayName("Should throw error when retrying non-failed task")
    void retryTask_notFailed() {
        // Given
        final String taskId = "completed-task";
        final Task task = createSampleTask(taskId, "EMAIL", TaskStatus.COMPLETED);
        final TaskEntity entity = TaskEntity.fromDomain(task);

        when(taskRepository.findById(taskId)).thenReturn(Mono.just(entity));

        // When
        final  Mono<TaskResult> result = taskAdminService.retryTask(taskId);

        // Then
        StepVerifier.create(result)
                .expectError(IllegalStateException.class)
                .verify();

        verify(taskExecutionEngine, never()).executeTask(any(), any());
    }

    @Test
    @DisplayName("Should return counts by task status")
    void countTasksByStatus() {
        // Given
        final TaskEntity pendingTask1 = TaskEntity.fromDomain(createSampleTask("task-1", "EMAIL", TaskStatus.PENDING));
        final TaskEntity pendingTask2 = TaskEntity.fromDomain(createSampleTask("task-2", "EMAIL", TaskStatus.PENDING));
        final TaskEntity completedTask = TaskEntity.fromDomain(createSampleTask("task-3", "EMAIL", TaskStatus.COMPLETED));

        when(taskRepository.findByType("EMAIL")).thenReturn(Flux.just(pendingTask1, pendingTask2, completedTask));

        // When
        final Mono<Map<TaskStatus, Long>> result = taskAdminService.countTasksByStatus(null, "EMAIL");

        // Then
        StepVerifier.create(result)
                .expectNextMatches(map ->
                        map.get(TaskStatus.PENDING) == 2L &&
                                map.get(TaskStatus.COMPLETED) == 1L)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should count by status with context ID filter")
    void countTasksByStatus_withContextId() {
        // Given
        final TaskEntity task1 = TaskEntity.fromDomain(createSampleTask("task-1", "EMAIL", TaskStatus.PENDING,
                Map.of("clientId", "client-123")));
        final TaskEntity task2 = TaskEntity.fromDomain(createSampleTask("task-2", "EMAIL", TaskStatus.PENDING,
                Map.of("clientId", "client-456"))); // Different client ID

        when(taskRepository.findAll()).thenReturn(Flux.just(task1, task2));

        // When
        final  Mono<Map<TaskStatus, Long>> result = taskAdminService.countTasksByStatus("client-123", null);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(map ->
                        map.get(TaskStatus.PENDING) == 1L) // Only one task matches the clientId
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return tasks associated with user ID")
    void getTasksByUserId_success() {
        // Given
        final String userId = "user-123";
        final TaskStatus status = TaskStatus.PENDING;
        int page = 0;
        int size = 10;

        final TaskEntity task1 = createTaskEntity("task-1", "DOCUMENT_PROCESSING",
                Map.of("userId", userId, "documentId", "doc-1"), TaskStatus.PENDING);
        final TaskEntity task2 = createTaskEntity("task-2", "EMAIL_NOTIFICATION",
                Map.of("userId", userId, "recipientEmail", "user@example.com"), TaskStatus.PENDING);
        final TaskEntity task3 = createTaskEntity("task-3", "DATA_IMPORT",
                Map.of("userIdentifier", "different-user"), TaskStatus.PENDING);

        when(taskRepository.findByStatus(eq(status), any(PageRequest.class)))
                .thenReturn(Flux.just(task1, task2, task3));

        // When
        final Flux<Task> result = taskAdminService.getTasksByUserId(userId, status, page, size, "createdAt", "desc");

        // Then
        StepVerifier.create(result)
                .expectNextMatches(task -> task.taskId().equals("task-1") &&
                        task.data().get("userId").equals(userId))
                .expectNextMatches(task -> task.taskId().equals("task-2") &&
                        task.data().get("userId").equals(userId))
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should handle different user ID field names")
    void getTasksByUserId_differentFieldNames() {
        // Given
        final String userId = "user-123";

        final TaskEntity task1 = createTaskEntity("task-1", "DOCUMENT_PROCESSING",
                Map.of("user_id", userId), TaskStatus.COMPLETED);
        final TaskEntity task2 = createTaskEntity("task-2", "EMAIL_NOTIFICATION",
                Map.of("uid", userId), TaskStatus.PENDING);
        final TaskEntity task3 = createTaskEntity("task-3", "PAYMENT_PROCESSING",
                Map.of("customerUserid", userId), TaskStatus.IN_PROGRESS);

        when(taskRepository.findAllWithPagination(any(PageRequest.class)))
                .thenReturn(Flux.just(task1, task2, task3));

        // When
        final Flux<Task> result = taskAdminService.getTasksByUserId(userId, null, 0, 20, "createdAt", "desc");

        // Then
        StepVerifier.create(result)
                .expectNextCount(3)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should return empty flux when user ID not found")
    void getTasksByUserId_userNotFound() {
        // Given
        final String userId = "non-existent";

        final TaskEntity task1 = createTaskEntity("task-1", "DOCUMENT_PROCESSING",
                Map.of("userId", "other-user"), TaskStatus.COMPLETED);
        final TaskEntity task2 = createTaskEntity("task-2", "EMAIL_NOTIFICATION",
                Map.of("uid", "another-user"), TaskStatus.PENDING);

        when(taskRepository.findAllWithPagination(any(PageRequest.class)))
                .thenReturn(Flux.just(task1, task2));

        // When
        final Flux<Task> result = taskAdminService.getTasksByUserId(userId, null, 0, 20, "createdAt", "desc");

        // Then
        StepVerifier.create(result)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should handle null user ID")
    void getTasksByUserId_nullUserId() {
        // When
        final Flux<Task> result = taskAdminService.getTasksByUserId(null, null, 0, 20, "createdAt", "desc");

        // Then
        StepVerifier.create(result)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should filter by both user ID and status")
    void getTasksByUserId_withStatusFilter() {
        // Given
        final String userId = "user-123";
        final TaskStatus status = TaskStatus.COMPLETED;

        final TaskEntity task1 = createTaskEntity("task-1", "DOCUMENT_PROCESSING",
                Map.of("userId", userId), TaskStatus.COMPLETED);
        final TaskEntity task2 = createTaskEntity("task-2", "EMAIL_NOTIFICATION",
                Map.of("userId", userId), TaskStatus.PENDING);
        final TaskEntity task3 = createTaskEntity("task-3", "DATA_IMPORT",
                Map.of("userId", userId), TaskStatus.COMPLETED);

        when(taskRepository.findByStatus(eq(status), any(PageRequest.class)))
                .thenReturn(Flux.just(task1, task3));

        // When
        final Flux<Task> result = taskAdminService.getTasksByUserId(userId, status, 0, 10, "createdAt", "desc");

        // Then
        StepVerifier.create(result)
                .expectNextCount(2)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should update task")
    void updateTask() {
        // Given
        final String taskId = "task-to-update";
        final Task task = createSampleTask(taskId, "EMAIL", TaskStatus.PENDING);
        final Task updatedTask = task.withStatus(TaskStatus.FAILED);

        final TaskEntity savedEntity = TaskEntity.fromDomain(updatedTask);

        when(taskRepository.save(any(TaskEntity.class))).thenReturn(Mono.just(savedEntity));

        // When
        final  Mono<Task> result = taskAdminService.updateTask(updatedTask);

        // Then
        StepVerifier.create(result)
                .expectNext(updatedTask)
                .verifyComplete();

        verify(taskRepository).save(taskEntityCaptor.capture());

        final TaskEntity capturedEntity = taskEntityCaptor.getValue();
        assertThat(capturedEntity.taskId()).isEqualTo(taskId);
        assertThat(capturedEntity.status()).isEqualTo(TaskStatus.FAILED);
    }

    private Task createSampleTask(String id, String type, TaskStatus status) {
        return createSampleTask(id, type, status, Map.of(), 0);
    }

    private Task createSampleTask(String id, String type, TaskStatus status, Map<String, Object> data) {
        return createSampleTask(id, type, status, data, 0);
    }

    private Task createSampleTask(String id, String type, TaskStatus status, Map<String, Object> data, int retryCount) {
        Instant now = Instant.now();
        return Task.builder()
                .taskId(id)
                .type(type)
                .status(status)
                .data(data)
                .retryCount(retryCount)
                .createdAt(now.minus(30, ChronoUnit.MINUTES))
                .updatedAt(now)
                .build();
    }

    private TaskEntity createTaskEntity(String id, String type, Map<String, Object> data, TaskStatus status) {
        final Task task = Task.builder()
                .taskId(id)
                .type(type)
                .data(data)
                .status(status)
                .retryCount(0)
                .createdAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .updatedAt(Instant.now().minus(30, ChronoUnit.MINUTES))
                .build();

        return TaskEntity.fromDomain(task);
    }
}

