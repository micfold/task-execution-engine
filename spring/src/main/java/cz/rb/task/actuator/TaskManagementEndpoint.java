package cz.rb.task.actuator;

import cz.rb.task.model.Task;
import cz.rb.task.model.TaskStatus;
import cz.rb.task.service.TaskAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Boot Actuator endpoint for task monitoring and management.
 * Provides system-level task operations for monitoring dashboards.
 *
 * @author micfold
 */
@Slf4j
@Component
@Endpoint(id = "tasks")
@ConditionalOnClass(name = "Endpoint")
@ConditionalOnProperty(name = "management.endpoint.tasks.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class TaskManagementEndpoint {

    private final TaskAdminService taskAdminService;

    /**
     * Gets detailed information about a specific task.
     *
     * @param taskId The task ID to query
     * @return Task details
     */
    @ReadOperation
    public Mono<Map<String, Object>> getTaskDetails(@Selector String taskId) {
        log.debug("Getting details for task: {} via actuator endpoint", taskId);

        return taskAdminService.getTaskById(taskId)
                .map(task -> {
                    Map<String, Object> details = new HashMap<>();
                    details.put("id", task.taskId());
                    details.put("type", task.type());
                    details.put("status", task.status());
                    details.put("retryCount", task.retryCount());
                    details.put("createdAt", task.createdAt());
                    details.put("updatedAt", task.updatedAt());
                    details.put("data", task.data());

                    // Calculate execution time if completed
                    if (task.status() == TaskStatus.COMPLETED && task.createdAt() != null) {
                        long executionTimeMs = task.updatedAt().toEpochMilli() -
                                task.createdAt().toEpochMilli();
                        details.put("executionTimeMs", executionTimeMs);
                        details.put("executionTimeFormatted",
                                String.format("%d.%03ds", executionTimeMs / 1000, executionTimeMs % 1000));
                    }

                    return details;
                });
    }

    /**
     * Gets a list of all tasks associated with a specific user.
     *
     * @param userId The user ID to query
     * @return List of tasks associated with the user
     */
    @ReadOperation
    public Flux<Map<String, Object>> getTaskListByUserId(@Selector String userId) {
        log.debug("Getting list of tasks for user: {} via actuator endpoint", userId);

        return taskAdminService.getTasksByUserId(userId, null, 0, 100, "createdAt", "desc")
                .map(task -> {
                    Map<String, Object> details = new HashMap<>();
                    details.put("id", task.taskId());
                    details.put("type", task.type());
                    details.put("status", task.status());
                    details.put("retryCount", task.retryCount());
                    details.put("createdAt", task.createdAt());
                    details.put("updatedAt", task.updatedAt());
                    details.put("data", task.data());

                    // Calculate execution time if completed
                    if (task.status() == TaskStatus.COMPLETED && task.createdAt() != null) {
                        long executionTimeMs = task.updatedAt().toEpochMilli() -
                                task.createdAt().toEpochMilli();
                        details.put("executionTimeMs", executionTimeMs);
                        details.put("executionTimeFormatted",
                                String.format("%d.%03ds", executionTimeMs / 1000, executionTimeMs % 1000));
                    }

                    return details;
                });
    }

    /**
     * Administratively marks a task as failed.
     * Useful for operational interventions.
     *
     * @param taskId The task ID to mark as failed
     * @return Result of the operation
     */
    @WriteOperation
    public Mono<Map<String, Object>> markTaskFailed(@Selector String taskId) {
        log.info("Administratively marking task as failed: {} via actuator endpoint", taskId);

        return taskAdminService.getTaskById(taskId)
                .flatMap(task -> {
                    // Check if task can be marked as failed
                    if (task.status() != TaskStatus.IN_PROGRESS && task.status() != TaskStatus.PENDING) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "Can only mark IN_PROGRESS or PENDING tasks as failed");
                        return Mono.just(response);
                    }

                    // Update task status to FAILED
                    final Task failedTask = task.withStatus(TaskStatus.FAILED);
                    return taskAdminService.updateTask(failedTask)
                            .map(updated -> {
                                Map<String, Object> response = new HashMap<>();
                                response.put("success", true);
                                response.put("message", "Task marked as failed");
                                response.put("taskId", taskId);
                                return response;
                            });
                })
                .defaultIfEmpty(new HashMap<String, Object>() {{
                    put("success", false);
                    put("message", "Task not found: " + taskId);
                }});
    }
}