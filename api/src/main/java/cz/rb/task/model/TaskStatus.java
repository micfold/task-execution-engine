package cz.rb.task.model;

/**
 * Enumeration of possible task statuses.
 * Represents the lifecycle states of a task.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 22.02.2025
 */
public enum TaskStatus {
    /** Task is created but not yet started */
    PENDING,

    /** Task is currently being executed */
    IN_PROGRESS,

    /** Task has been successfully completed */
    COMPLETED,

    /** Task execution has failed but may be retried */
    FAILED,

    /** Task has permanently failed and been moved to DLQ */
    DEAD_LETTER;

    /**
     * Checks if the task status is terminal.
     *
     * @return true if the task status is COMPLETED or DEAD_LETTER, false otherwise
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == DEAD_LETTER;
    }
}
