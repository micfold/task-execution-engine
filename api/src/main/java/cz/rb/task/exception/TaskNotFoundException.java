package cz.rb.task.exception;

/**
 * Exception thrown when a requested task cannot be found.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 22.02.2025
 */
public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(String taskId) {
        super("Task not found with ID: " + taskId);
    }
}
