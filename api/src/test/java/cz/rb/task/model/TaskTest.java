package cz.rb.task.model;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Task domain model.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 22.02.2025
 */
class TaskTest {
    private static final String TASK_ID = UUID.randomUUID().toString();
    private static final String TASK_TYPE = "TEST_TASK";
    private static final Map<String, Object> TASK_DATA = Map.of("key", "value");

    @Test
    void shouldCreateTaskWithBuilder() {
        // When
        final Task task = Task.builder()
                .taskId(TASK_ID)
                .type(TASK_TYPE)
                .data(TASK_DATA)
                .status(TaskStatus.PENDING)
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Then
        assertNotNull(task);
        assertEquals(TASK_ID, task.taskId());
        assertEquals(TASK_TYPE, task.type());
        assertEquals(TASK_DATA, task.data());
        assertEquals(TaskStatus.PENDING, task.status());
        assertEquals(0, task.retryCount());
        assertNotNull(task.createdAt());
        assertNotNull(task.updatedAt());
    }

    @SneakyThrows
    @Test
    void shouldUpdateStatusAndTimestamp() {
        // Given
        final Task task = createTestTask();
        final Instant originalUpdatedAt = task.updatedAt();
        Thread.sleep(1); // Ensure timestamp difference

        // When
        final Task updatedTask = task.withStatus(TaskStatus.IN_PROGRESS);

        // Then
        assertNotEquals(task.status(), updatedTask.status());
        assertEquals(TaskStatus.IN_PROGRESS, updatedTask.status());
        assertTrue(updatedTask.updatedAt().isAfter(originalUpdatedAt));

        // Other fields should remain unchanged
        assertEquals(task.taskId(), updatedTask.taskId());
        assertEquals(task.type(), updatedTask.type());
        assertEquals(task.data(), updatedTask.data());
        assertEquals(task.retryCount(), updatedTask.retryCount());
        assertEquals(task.createdAt(), updatedTask.createdAt());
    }

    @SneakyThrows
    @Test
    void shouldIncrementRetryCount() {
        // given
        final Task task = createTestTask();
        final Instant originalUpdatedAt = task.updatedAt();
        Thread.sleep(1); // Ensure timestamp difference

        // when
        final Task retriedTask = task.incrementRetry();

        // then
        assertEquals(task.retryCount() + 1, retriedTask.retryCount());
        assertTrue(retriedTask.updatedAt().isAfter(originalUpdatedAt));

        // Other fields should remain unchanged
        assertEquals(task.taskId(), retriedTask.taskId());
        assertEquals(task.type(), retriedTask.type());
        assertEquals(task.data(), retriedTask.data());
        assertEquals(task.status(), retriedTask.status());
        assertEquals(task.createdAt(), retriedTask.createdAt());
    }

    private Task createTestTask() {
        return Task.builder()
                .taskId(TASK_ID)
                .type(TASK_TYPE)
                .data(TASK_DATA)
                .status(TaskStatus.PENDING)
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
