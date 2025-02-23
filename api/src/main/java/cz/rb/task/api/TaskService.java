package cz.rb.task.api;

import cz.rb.task.model.Task;
import cz.rb.task.model.TaskResult;
import reactor.core.publisher.Mono;

/**
 * Core interface for executing tasks in the system.
 * This service handles task submission, execution, and status tracking.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 22.02.2025
 */
public interface TaskService {
    /**
     * Submits a task for execution.
     *
     * @param task The task to be executed
     * @return A Mono containing the result of the task
     */
    Mono<TaskResult> submitTask(final Task task);

    /**
     * Retrieves the current status of a task.
     *
     * @param taskId The unique identifier of the task
     * @return A Mono containing the task, if found
     */
    Mono<Task> getTaskById(final String taskId);
}
