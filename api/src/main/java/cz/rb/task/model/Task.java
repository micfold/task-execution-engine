package cz.rb.task.model;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a task in the system.
 * Tasks are units of work that can be executed asynchronously.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 22.02.2025
 */
@Builder
public record Task(
        String taskId,
        String type,
        Map<String, Object> data,
        TaskStatus status,
        int retryCount,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Creates a new Task with an updated status.
     *
     * @param newStatus The new status to apply
     * @return A new Task instance with updated status and timestamp
     */
    public Task withStatus(final TaskStatus newStatus) {
        return new Task(
                taskId,
                type,
                data,
                newStatus, // Use the new status
                retryCount,
                createdAt,
                Instant.now() // Update timestamp
        );
    }

    /**
     * Creates a new Task with incremented retry count.
     *
     * @return A new Task instance with incremented retry count
     */
    public Task incrementRetry() {
        return new Task(
                taskId,
                type,
                data,
                status,
                retryCount + 1, // Increment retry count
                createdAt,
                Instant.now() // Update timestamp
        );
    }
}
