package cz.rb.task.service;

import cz.rb.task.engine.DefaultTaskExecutionEngine;
import cz.rb.task.engine.TaskHandlerRegistry;
import cz.rb.task.exception.TaskNotFoundException;
import cz.rb.task.model.Task;
import cz.rb.task.model.TaskResult;
import cz.rb.task.model.TaskStatus;
import cz.rb.task.persistence.TaskEntity;
import cz.rb.task.persistence.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for task administration and management.
 * Provides task query, retry, and maintenance capabilities.
 *
 * @author micfold
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskAdminService {

    private final TaskRepository taskRepository;
    private final TaskHandlerRegistry taskHandlerRegistry;
    private final DefaultTaskExecutionEngine taskExecutionEngine;

    /**
     * Finds tasks with optional filtering and pagination.
     *
     * @param contextId  Optional parent context identifier
     * @param status     Optional task status filter
     * @param type       Optional task type filter
     * @param startDate  Optional start date filter
     * @param endDate    Optional end date filter
     * @param page       Page number (0-based)
     * @param size       Page size
     * @param sortBy     Field to sort by
     * @param sortDir    Sort direction (asc or desc)
     * @return Flux of matching tasks
     */
    public Flux<Task> findTasks(
            final String contextId,
            final TaskStatus status,
            final String type,
            final Instant startDate,
            final Instant endDate,
            final int page,
            final int size,
            final String sortBy,
            final String sortDir
    ) {
        log.debug("Finding tasks with filters: contextId={}, status={}, type={}, startDate={}, endDate={}, page={}, size={}",
                contextId, status, type, startDate, endDate, page, size);

        // Validate and default sorting
        final String validSortBy = validateSortField(sortBy);
        final Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        final PageRequest pageRequest = PageRequest.of(page, size, direction, validSortBy);

        // Build query based on provided filters
        Flux<TaskEntity> taskEntities;

        if (type != null && status != null) {
            taskEntities = taskRepository.findByTypeAndStatus(type, status, pageRequest);
        } else if (status != null) {
            taskEntities = taskRepository.findByStatus(status, pageRequest);
        } else if (type != null) {
            taskEntities = taskRepository.findByType(type, pageRequest);
        } else {
            // If no type or status, use findAll when filtering by contextId
            if (contextId != null) {
                taskEntities = taskRepository.findAll();
            } else {
                taskEntities = taskRepository.findAllWithPagination(pageRequest);
            }
        }

        // Apply context filter if provided (generic approach)
        if (contextId != null && taskEntities != null) {
            taskEntities = taskEntities.filter(entity ->
                    containsContextId(entity.data(), contextId));
        }

// Apply date filters if provided
        if (startDate != null && taskEntities != null) {
            taskEntities = taskEntities.filter(entity -> !entity.createdAt().isBefore(startDate));
        }
        if (endDate != null && taskEntities != null) {
            taskEntities = taskEntities.filter(entity -> !entity.createdAt().isAfter(endDate));
        }

        return taskEntities.map(TaskEntity::toDomain);
    }

    /**
     * Gets a task by ID.
     *
     * @param taskId The task ID
     * @return Mono containing the task, if found
     */
    public Mono<Task> getTaskById(final String taskId) {
        log.debug("Getting task by ID: {}", taskId);

        return taskRepository.findById(taskId)
                .map(TaskEntity::toDomain)
                .switchIfEmpty(Mono.error(new TaskNotFoundException(taskId)));
    }

    /**
     * Retries a failed task.
     *
     * @param taskId The task ID
     * @return The result of the retry attempt
     */
    public Mono<TaskResult> retryTask(final String taskId) {
        log.info("Retrying task: {}", taskId);

        return taskRepository.findById(taskId)
                .switchIfEmpty(Mono.error(new TaskNotFoundException(taskId)))
                .flatMap(taskEntity -> {
                    final Task task = taskEntity.toDomain();

                    // Validate task status
                    if (task.status() != TaskStatus.FAILED && task.status() != TaskStatus.DEAD_LETTER) {
                        return Mono.error(new IllegalStateException(
                                "Task cannot be retried from status: " + task.status()));
                    }

                    // Find handler for this task type
                    return taskHandlerRegistry.getHandler(task.type())
                            .switchIfEmpty(Mono.error(new IllegalStateException(
                                    "No handler found for task type: " + task.type())))
                            .flatMap(handler -> {
                                // Reset status to PENDING
                                final Task retryTask = task.withStatus(TaskStatus.PENDING);

                                // Execute through the engine
                                return taskExecutionEngine.executeTask(retryTask, handler);
                            });
                });
    }

    /**
     * Counts tasks by status with optional filtering.
     *
     * @param contextId Optional parent context identifier
     * @param type Optional task type filter
     * @return Map of status to count
     */
    public Mono<Map<TaskStatus, Long>> countTasksByStatus(final String contextId, final String type) {
        log.debug("Counting tasks by status with contextId={}, type={}", contextId, type);

        // If specific filtering is needed, build the query
        Flux<TaskEntity> allTasks;
        if (type != null) {
            allTasks = taskRepository.findByType(type);
        } else {
            allTasks = taskRepository.findAll();
        }

        // Apply context filter if provided
        if (contextId != null) {
            allTasks = allTasks.filter(entity ->
                    containsContextId(entity.data(), contextId));
        }

        // Group by status and count
        return allTasks
                .collectMultimap(TaskEntity::status)
                .map(map -> map.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> (long) entry.getValue().size()
                        )));
    }

    /**
     * Updates a task with new state.
     * This is useful for administrative operations like changing task status.
     *
     * @param task The task with updated properties
     * @return The updated task
     */
    public Mono<Task> updateTask(final Task task) {
        log.debug("Updating task: {}, status: {}", task.taskId(), task.status());

        // Ensure updatedAt is set to current time
        Task updatedTask = task;
        if (task.updatedAt() == null || task.updatedAt().equals(task.createdAt())) {
            updatedTask = Task.builder()
                    .taskId(task.taskId())
                    .type(task.type())
                    .data(task.data())
                    .status(task.status())
                    .retryCount(task.retryCount())
                    .createdAt(task.createdAt())
                    .updatedAt(Instant.now())
                    .build();
        }

        // Convert to entity and save
        TaskEntity entity = TaskEntity.fromDomain(updatedTask);
        return taskRepository.save(entity)
                .map(TaskEntity::toDomain);
    }

    /**
     * Helper method to check if task data contains a specific context ID.
     * This is a generic approach that works with various parent contexts.
     */
    private boolean containsContextId(Map<String, Object> data, String contextId) {
        if (data == null) {
            return false;
        }

        // Check common context ID fields
        return data.entrySet().stream()
                .anyMatch(entry -> {
                    String key = entry.getKey().toLowerCase();
                    Object value = entry.getValue();

                    return (key.contains("id") || key.endsWith("id")) &&
                            value != null &&
                            value.toString().equals(contextId);
                });
    }

    /**
     * Validates and normalizes sort field name.
     *
     * @param sortBy The requested sort field
     * @return Valid sort field name
     */
    private String validateSortField(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "id", "taskid" -> "task_id";
            case "type" -> "type";
            case "status" -> "status";
            case "retrycount" -> "retry_count";
            case "createdat" -> "created_at";
            case "updatedat" -> "updated_at";
            default -> "created_at"; // Default sort
        };
    }
}