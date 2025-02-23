package cz.rb.task.exception;

/**
 * Exception thrown when task execution fails.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 22.02.2025
 */
public class TaskExecutionException extends RuntimeException {
    public TaskExecutionException(String message) {
        super(message);
    }

    public TaskExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
