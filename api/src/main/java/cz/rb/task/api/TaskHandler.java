package cz.rb.task.api;

import cz.rb.task.model.Task;
import cz.rb.task.model.TaskResult;
import reactor.core.publisher.Mono;

/**
 * Contract for task execution handlers.
 * Implementations provide the actual execution logic for specific task types.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 22.02.2025
 */
public interface TaskHandler {
    /**
     * Executes the given task and returns its result.
     *
     * @param task The task to execute
     * @return A Mono containing the result of the task
     */
    Mono<TaskResult> execute(final Task task);

    /**
     * Gets the type of tasks this handler can process.
     *
     * @return The task type identifier
     */
    String getTaskType();
}
