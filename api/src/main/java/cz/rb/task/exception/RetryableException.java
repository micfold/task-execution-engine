package cz.rb.task.exception;

/**
 * Exception indicating that a task execution failure is temporary and can be retried.
 * This exception is used to distinguish between transient failures that should trigger
 * a retry attempt and permanent failures that should not be retried.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 22.02.2025
 */
public class RetryableException extends RuntimeException {
    /**
     * Constructs a new RetryableException with the specified detail message.
     *
     * @param message The detail message
     */
    public RetryableException(String message) {
        super(message);
    }

    /**
     * Constructs a new RetryableException with the specified detail message and cause.
     *
     * @param message The detail message
     * @param cause The cause of the exception
     */
    public RetryableException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new RetryableException with the specified cause.
     *
     * @param cause The cause of the exception
     */
    public RetryableException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new RetryableException with the specified detail message, cause,
     * suppression enabled or disabled, and writable stack trace enabled or disabled.
     *
     * @param message The detail message
     * @param cause The cause of the exception
     * @param enableSuppression Whether suppression is enabled or disabled
     * @param writableStackTrace Whether the stack trace should be writable
     */
    public RetryableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
