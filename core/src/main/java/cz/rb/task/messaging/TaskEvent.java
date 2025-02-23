package cz.rb.task.messaging;

import java.time.Instant;
import java.util.Map;

/**
 * Represents an event in the task execution lifecycle.
 * Events are published to Kafka for tracking task progress and state changes.
 *

 * @Project: edi-task-execution-engine
 * @Author: micfold on 22.02.2025
 *
 * @param taskId Unique identifier of the task
 * @param taskType Type of the task
 * @param eventType Type of event (e.g., STARTED, COMPLETED, FAILED)
 * @param metadata Additional event-specific data
 * @param timestamp When the event occurred
 */
public record TaskEvent(
        String taskId,
        String taskType,
        String eventType,
        Map<String, Object> metadata,
        Instant timestamp
) {
    /**
     * Event type constants for task lifecycle events
     */
    public static final class EventTypes {
        public static final String TASK_CREATED = "TASK_CREATED";
        public static final String TASK_STARTED = "TASK_STARTED";
        public static final String TASK_COMPLETED = "TASK_COMPLETED";
        public static final String TASK_FAILED = "TASK_FAILED";
        public static final String MOVED_TO_DLQ = "MOVED_TO_DLQ";

        private EventTypes() {}
    }

    /**
     * Factory method for task started event.
     */
    public static TaskEvent taskStarted(String taskId, String taskType) {
        return new TaskEvent(taskId, taskType, EventTypes.TASK_STARTED,
                Map.of(), Instant.now());
    }

    /**
     * Factory method for task completed event.
     */
    public static TaskEvent taskCompleted(String taskId, String taskType,
                                          Map<String, Object> result) {
        return new TaskEvent(taskId, taskType, EventTypes.TASK_COMPLETED,
                result, Instant.now());
    }

    /**
     * Factory method for task failed event.
     */
    public static TaskEvent taskFailed(String taskId, String taskType,
                                       String error, boolean retryable) {
        return new TaskEvent(taskId, taskType, EventTypes.TASK_FAILED,
                Map.of(
                        "error", error,
                        "retryable", retryable
                ),
                Instant.now());
    }
}