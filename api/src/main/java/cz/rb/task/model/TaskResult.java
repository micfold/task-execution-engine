package cz.rb.task.model;

import java.util.Map;

/**
 * Sealed interface representing the result of a task execution.
 * Can be either Success or Failure.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 22.02.2025
 */
public sealed interface TaskResult {
    /**
     * Gets the ID of the task this result is for.
     *
     * @return The task ID
     */
    String taskId();

    /**
     * Represents a successful task execution.
     *
     * @param taskId The ID of the completed task
     * @param result The data resulting from the task execution
     */
    record Success(
            String taskId,
            Map<String, Object> result
    ) implements TaskResult {}

    /**
     * Represents a failed task execution.
     *
     * @param taskId The ID of the failed task
     * @param error Description of what went wrong
     * @param retryable Whether the task can be retried
     */
    record Failure(
            String taskId,
            String error,
            boolean retryable
    ) implements TaskResult {}
}
