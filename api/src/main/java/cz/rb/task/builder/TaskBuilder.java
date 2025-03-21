package cz.rb.task.builder;

import cz.rb.task.model.Task;
import cz.rb.task.model.TaskStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Convenience builder for creating Task objects with standard defaults.
 * Simplifies task creation for component developers by handling common patterns.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 21.03.2025
 */
public class TaskBuilder {
    private String taskId;
    private final String type;
    private final Map<String, Object> data = new HashMap<>();
    private TaskStatus status = TaskStatus.PENDING;
    private int retryCount = 0;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Creates a TaskBuilder for the specified task type.
     *
     * @param type The task type identifier
     */
    private TaskBuilder(final String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Task type cannot be null or blank");
        }
        this.type = type;
        this.taskId = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }
    /**
     * Creates a new TaskBuilder for the specified task type.
     *
     * @param type The task type identifier
     * @return A new TaskBuilder
     */
    public static TaskBuilder forType(String type) {
        return new TaskBuilder(type);
    }

    /**
     * Sets a custom task ID.
     * If not called, a random UUID will be used.
     *
     * @param taskId The task ID to use
     * @return This builder for method chaining
     */
    public TaskBuilder withTaskId(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("Task ID cannot be null or blank");
        }
        this.taskId = taskId;
        return this;
    }

    /**
     * Adds a single data entry to the task payload.
     *
     * @param key The data key
     * @param value The data value
     * @return This builder for method chaining
     */
    public TaskBuilder withData(String key, Object value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Data key cannot be null or blank");
        }
        this.data.put(key, value);
        return this;
    }

    /**
     * Sets multiple data entries for the task payload.
     *
     * @param data The data map
     * @return This builder for method chaining
     */
    public TaskBuilder withData(Map<String, Object> data) {
        if (data != null) {
            this.data.putAll(data);
        }
        return this;
    }

    /**
     * Sets a non-default initial status.
     * If not called, status will be PENDING.
     *
     * @param status The initial task status
     * @return This builder for method chaining
     */
    public TaskBuilder withStatus(TaskStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Task status cannot be null");
        }
        this.status = status;
        return this;
    }

    /**
     * Sets a specific retry count.
     * If not called, retry count will be 0.
     *
     * @param retryCount The retry count
     * @return This builder for method chaining
     */
    public TaskBuilder withRetryCount(int retryCount) {
        if (retryCount < 0) {
            throw new IllegalArgumentException("Retry count cannot be negative");
        }
        this.retryCount = retryCount;
        return this;
    }

    /**
     * Sets specific created and updated timestamps.
     * If not called, current time will be used for both.
     *
     * @param createdAt The creation timestamp
     * @param updatedAt The update timestamp
     * @return This builder for method chaining
     */
    public TaskBuilder withTimestamps(Instant createdAt, Instant updatedAt) {
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("Timestamps cannot be null");
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Updated timestamp cannot be before created timestamp");
        }
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        return this;
    }

    /**
     * Builds the Task object.
     *
     * @return A new Task instance
     */
    public Task build() {
        return Task.builder()
                .taskId(taskId)
                .type(type)
                .data(new HashMap<>(data))  // Create a defensive copy
                .status(status)
                .retryCount(retryCount)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
