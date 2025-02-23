package cz.rb.task.engine;

import cz.rb.task.api.TaskHandler;
import cz.rb.task.model.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskHandlerRegistryTest {

    private TaskHandlerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new TaskHandlerRegistry();
    }

    @Test
    void registerHandler_Success() {
        // Given
        TaskHandler handler = mockTaskHandler("TEST_TASK");

        // When
        registry.registerHandler(handler);

        // Then
        assertTrue(registry.hasHandler("TEST_TASK"));
        assertEquals(1, registry.getHandlerCount());
    }

    @Test
    void registerHandler_NullHandler() {
        // When/Then
        assertThrows(IllegalArgumentException.class,
                () -> registry.registerHandler(null));
    }

    @Test
    void registerHandler_OverwriteExisting() {
        // Given
        TaskHandler handler1 = mockTaskHandler("TEST_TASK");
        TaskHandler handler2 = mockTaskHandler("TEST_TASK");

        // When
        registry.registerHandler(handler1);
        registry.registerHandler(handler2);

        // Then
        assertTrue(registry.hasHandler("TEST_TASK"));
        assertEquals(1, registry.getHandlerCount());
    }

    @Test
    void getHandler_ExistingHandler() {
        // Given
        TaskHandler handler = mockTaskHandler("TEST_TASK");
        registry.registerHandler(handler);

        // When
        Mono<TaskHandler> result = registry.getHandler("TEST_TASK");

        // Then
        StepVerifier.create(result)
                .expectNext(handler)
                .verifyComplete();
    }

    @Test
    void getHandler_NonExistent() {
        // When
        Mono<TaskHandler> result = registry.getHandler("NON_EXISTENT");

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void getHandler_NullTaskType() {
        // When/Then
        assertThrows(IllegalArgumentException.class,
                () -> registry.getHandler(null));
    }

    @Test
    void getHandler_BlankTaskType() {
        // When/Then
        assertThrows(IllegalArgumentException.class,
                () -> registry.getHandler("  "));
    }

    @Test
    void removeHandler_Success() {
        // Given
        TaskHandler handler = mockTaskHandler("TEST_TASK");
        registry.registerHandler(handler);

        // When
        boolean removed = registry.removeHandler("TEST_TASK");

        // Then
        assertTrue(removed);
        assertFalse(registry.hasHandler("TEST_TASK"));
        assertEquals(0, registry.getHandlerCount());
    }

    @Test
    void removeHandler_NonExistent() {
        // When
        boolean removed = registry.removeHandler("NON_EXISTENT");

        // Then
        assertFalse(removed);
    }

    @Test
    void removeHandler_NullTaskType() {
        // When/Then
        assertThrows(IllegalArgumentException.class,
                () -> registry.removeHandler(null));
    }

    @Test
    void clearHandlers() {
        // Given
        registry.registerHandler(mockTaskHandler("TASK1"));
        registry.registerHandler(mockTaskHandler("TASK2"));
        registry.registerHandler(mockTaskHandler("TASK3"));

        // When
        registry.clearHandlers();

        // Then
        assertEquals(0, registry.getHandlerCount());
        assertFalse(registry.hasHandler("TASK1"));
        assertFalse(registry.hasHandler("TASK2"));
        assertFalse(registry.hasHandler("TASK3"));
    }

    private TaskHandler mockTaskHandler(String taskType) {
        TaskHandler handler = mock(TaskHandler.class);
        when(handler.getTaskType()).thenReturn(taskType);
        when(handler.execute(any())).thenReturn(
                Mono.just(new TaskResult.Success(UUID.randomUUID().toString(), Map.of())));
        return handler;
    }
}