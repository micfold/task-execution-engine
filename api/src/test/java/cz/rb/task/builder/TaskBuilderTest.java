package cz.rb.task.builder;

import cz.rb.task.model.Task;
import cz.rb.task.model.TaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TaskBuilder}.
 *
 * @Project: edi-task-execution-engine
 * @Author: Claude 3.7 Sonnet on 12.04.2025
 */
class TaskBuilderTest {

    private static final String TASK_TYPE = "TEST_TASK";
    private static final String CUSTOM_TASK_ID = "custom-task-id";

    @Test
    @DisplayName("Should create task with default values")
    void createTaskWithDefaults() {
        // When
        Task task = TaskBuilder.forType(TASK_TYPE).build();

        // Then
        assertNotNull(task);
        assertNotNull(task.taskId());
        assertEquals(TASK_TYPE, task.type());
        assertEquals(TaskStatus.PENDING, task.status());
        assertEquals(0, task.retryCount());
        assertNotNull(task.createdAt());
        assertNotNull(task.updatedAt());
        assertEquals(task.createdAt(), task.updatedAt());
        assertNotNull(task.data());
        assertTrue(task.data().isEmpty());
    }

    @ParameterizedTest
    @DisplayName("Should throw exception for invalid task type")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void invalidTaskType(String invalidType) {
        // Then
        assertThrows(IllegalArgumentException.class, () -> TaskBuilder.forType(invalidType));
    }

    @Test
    @DisplayName("Should set custom task ID")
    void withCustomTaskId() {
        // When
        Task task = TaskBuilder.forType(TASK_TYPE)
                .withTaskId(CUSTOM_TASK_ID)
                .build();

        // Then
        assertEquals(CUSTOM_TASK_ID, task.taskId());
    }

    @ParameterizedTest
    @DisplayName("Should throw exception for invalid task ID")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void invalidTaskId(String invalidId) {
        // Then
        assertThrows(IllegalArgumentException.class,
                () -> TaskBuilder.forType(TASK_TYPE).withTaskId(invalidId));
    }

    @Test
    @DisplayName("Should add single data entry")
    void withSingleDataEntry() {
        // Given
        String key = "testKey";
        String value = "testValue";

        // When
        Task task = TaskBuilder.forType(TASK_TYPE)
                .withData(key, value)
                .build();

        // Then
        assertNotNull(task.data());
        assertEquals(1, task.data().size());
        assertEquals(value, task.data().get(key));
    }

    @ParameterizedTest
    @DisplayName("Should throw exception for invalid data key")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void invalidDataKey(String invalidKey) {
        // Then
        assertThrows(IllegalArgumentException.class,
                () -> TaskBuilder.forType(TASK_TYPE).withData(invalidKey, "value"));
    }

    @Test
    @DisplayName("Should add multiple data entries")
    void withMultipleDataEntries() {
        // Given
        Map<String, Object> data = Map.of(
                "key1", "value1",
                "key2", 42,
                "key3", true
        );

        // When
        Task task = TaskBuilder.forType(TASK_TYPE)
                .withData(data)
                .build();

        // Then
        assertNotNull(task.data());
        assertEquals(3, task.data().size());
        assertEquals("value1", task.data().get("key1"));
        assertEquals(42, task.data().get("key2"));
        assertEquals(true, task.data().get("key3"));
    }

    @Test
    @DisplayName("Should handle null data map gracefully")
    void withNullDataMap() {
        // When
        Task task = TaskBuilder.forType(TASK_TYPE)
                .withData((Map<String, Object>) null)
                .build();

        // Then
        assertNotNull(task.data());
        assertTrue(task.data().isEmpty());
    }

    @Test
    @DisplayName("Should create defensive copy of data map")
    void createDefensiveCopyOfData() {
        // Given
        Map<String, Object> originalData = new HashMap<>();
        originalData.put("key", "original");

        // When
        Task task = TaskBuilder.forType(TASK_TYPE)
                .withData(originalData)
                .build();
        originalData.put("key", "modified");
        originalData.put("newKey", "newValue");

        // Then
        assertEquals("original", task.data().get("key"),
                "Task data should not be affected by modifications to original map");
        assertEquals(1, task.data().size(),
                "Task data should not reflect additions to original map");
    }

    @Test
    @DisplayName("Should set custom status")
    void withCustomStatus() {
        // When
        Task task = TaskBuilder.forType(TASK_TYPE)
                .withStatus(TaskStatus.IN_PROGRESS)
                .build();

        // Then
        assertEquals(TaskStatus.IN_PROGRESS, task.status());
    }

    @Test
    @DisplayName("Should throw exception for null status")
    void nullStatus() {
        // Then
        assertThrows(IllegalArgumentException.class,
                () -> TaskBuilder.forType(TASK_TYPE).withStatus(null));
    }

    @Test
    @DisplayName("Should set custom retry count")
    void withCustomRetryCount() {
        // Given
        int retryCount = 5;

        // When
        Task task = TaskBuilder.forType(TASK_TYPE)
                .withRetryCount(retryCount)
                .build();

        // Then
        assertEquals(retryCount, task.retryCount());
    }

    @Test
    @DisplayName("Should throw exception for negative retry count")
    void negativeRetryCount() {
        // Then
        assertThrows(IllegalArgumentException.class,
                () -> TaskBuilder.forType(TASK_TYPE).withRetryCount(-1));
    }

    @Test
    @DisplayName("Should set custom timestamps")
    void withCustomTimestamps() {
        // Given
        Instant created = Instant.now().minusSeconds(60);
        Instant updated = Instant.now();

        // When
        Task task = TaskBuilder.forType(TASK_TYPE)
                .withTimestamps(created, updated)
                .build();

        // Then
        assertEquals(created, task.createdAt());
        assertEquals(updated, task.updatedAt());
    }

    @Test
    @DisplayName("Should throw exception when updated timestamp is before created timestamp")
    void invalidTimestampOrder() {
        // Given
        Instant now = Instant.now();
        Instant before = now.minusSeconds(60);

        // Then - updated before created
        assertThrows(IllegalArgumentException.class,
                () -> TaskBuilder.forType(TASK_TYPE).withTimestamps(now, before));
    }

    @Test
    @DisplayName("Should throw exception for null timestamps")
    void nullTimestamps() {
        // Given
        Instant now = Instant.now();

        // Then
        assertThrows(IllegalArgumentException.class,
                () -> TaskBuilder.forType(TASK_TYPE).withTimestamps(null, now));
        assertThrows(IllegalArgumentException.class,
                () -> TaskBuilder.forType(TASK_TYPE).withTimestamps(now, null));
        assertThrows(IllegalArgumentException.class,
                () -> TaskBuilder.forType(TASK_TYPE).withTimestamps(null, null));
    }

    @Test
    @DisplayName("Should support chaining all builder methods")
    void chainAllMethods() {
        // Given
        String customId = UUID.randomUUID().toString();
        Map<String, Object> data = Map.of("key", "value");
        Instant created = Instant.now().minusSeconds(60);
        Instant updated = Instant.now();

        // When
        Task task = TaskBuilder.forType(TASK_TYPE)
                .withTaskId(customId)
                .withData("singleKey", "singleValue")
                .withData(data)
                .withStatus(TaskStatus.COMPLETED)
                .withRetryCount(3)
                .withTimestamps(created, updated)
                .build();

        // Then
        assertEquals(customId, task.taskId());
        assertEquals(TASK_TYPE, task.type());
        assertEquals(2, task.data().size());
        assertEquals("singleValue", task.data().get("singleKey"));
        assertEquals("value", task.data().get("key"));
        assertEquals(TaskStatus.COMPLETED, task.status());
        assertEquals(3, task.retryCount());
        assertEquals(created, task.createdAt());
        assertEquals(updated, task.updatedAt());
    }

    @Test
    @DisplayName("Should preserve method execution order")
    void preserveMethodExecutionOrder() {
        // When
        Task task = TaskBuilder.forType(TASK_TYPE)
                .withTaskId("firstId")
                .withTaskId("secondId")
                .withData("key1", "value1")
                .withData("key1", "value2")
                .build();

        // Then
        assertEquals("secondId", task.taskId(), "Last ID set should be used");
        assertEquals("value2", task.data().get("key1"), "Last value set should be used");
    }


    @Test
    @DisplayName("Should create task with random UUID if no ID is set")
    void createTaskWithRandomUUID() {
        // When
        Task task = TaskBuilder.forType(TASK_TYPE).build();

        // Then
        assertNotNull(task.taskId());
        assertNotEquals(CUSTOM_TASK_ID, task.taskId(), "Task ID should be different from custom ID");
    }
}