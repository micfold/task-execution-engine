package cz.rb.task.engine;

import cz.rb.task.api.TaskHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for task handlers that manages registration and lookup of handlers by task type.
 * Provides thread-safe access to task handlers.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 22.02.2025
 */
@Slf4j
@Component
public class TaskHandlerRegistry {

    private final Map<String, TaskHandler> handlers;

    public TaskHandlerRegistry() {
        this.handlers = new ConcurrentHashMap<>();
    }

    /**
     * Registers a new task handler.
     * If a handler for the same task type already exists, it will be overwritten.
     *
     * @param handler The task handler to register
     */
    public void registerHandler(final TaskHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }

        final String taskType = handler.getTaskType();
        final TaskHandler existing = handlers.put(taskType, handler);

        if (existing != null) {
            log.warn("Overwriting existing handler for task type: {}", taskType);
        }
        log.info("Registered handler for task type: {}", taskType);
    }

    /**
     * Retrieves a handler for the specified task type.
     *
     * @param taskType The type of task to find a handler for
     * @return A Mono containing the handler if found, empty if not found
     */
    public Mono<TaskHandler> getHandler(final String taskType) {
        validateTaskType(taskType);

        final TaskHandler handler = handlers.get(taskType);

        if (handler == null) {
            log.warn("No handler found for task type: {}", taskType);
            return Mono.empty();
        }

        return Mono.just(handler);
    }

    /**
     * Removes a handler for the specified task type.
     *
     * @param taskType The type of task whose handler should be removed
     * @return true if a handler was removed, false if no handler existed
     */
    public boolean removeHandler(final String taskType) {
        validateTaskType(taskType);

        final TaskHandler removed = handlers.remove(taskType);

        if (removed != null) {
            log.info("Removed handler for task type: {}", taskType);
            return true;
        }
        return false;
    }

    /**
     * Checks if a handler exists for the specified task type.
     *
     * @param taskType The task type to check
     * @return true if a handler exists, false otherwise
     */
    public boolean hasHandler(final String taskType) {
        if (taskType == null || taskType.isBlank()) {
            return false;
        }
        return handlers.containsKey(taskType);
    }

    /**
     * Gets the number of registered handlers.
     *
     * @return The count of registered handlers
     */
    public int getHandlerCount() {
        return handlers.size();
    }

    /**
     * Clears all registered handlers.
     * Primarily used for testing and system shutdown.
     */
    public void clearHandlers() {
        handlers.clear();
        log.info("Cleared all registered handlers");
    }

    /**
     * Validates a handler before registration.
     *
     * @param handler The handler to validate
     * @throws IllegalArgumentException if handler is invalid
     */
    private void validateHandler(final TaskHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }

        final String taskType = handler.getTaskType();
        validateTaskType(taskType);
    }

    /**
     * Validates a task type.
     *
     * @param taskType The task type to validate
     * @throws IllegalArgumentException if task type is invalid
     */
    private void validateTaskType(final String taskType) {
        if (taskType == null || taskType.isBlank()) {
            throw new IllegalArgumentException("Task type cannot be null or blank");
        }
    }


}
