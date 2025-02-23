package cz.rb.task.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TaskResult sealed interface and its implementations.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 22.02.2025
 */
class TaskResultTest {
    private static final String TASK_ID = "test-task";
    private static final Map<String, Object> SUCCESS_RESULT = Map.of("result", "success");
    private static final String ERROR_MESSAGE = "Test error";

    @Test
    void shouldCreateSuccessResult() {
        // When
        final TaskResult.Success success = new TaskResult.Success(TASK_ID, SUCCESS_RESULT);

        // Then
        assertEquals(TASK_ID, success.taskId());
        assertEquals(SUCCESS_RESULT, success.result());
    }

    @Test
    void shouldCreateFailureResult() {
        // When
        final TaskResult.Failure failure = new TaskResult.Failure(TASK_ID, ERROR_MESSAGE, true);

        // then
        assertEquals(TASK_ID, failure.taskId());
        assertEquals(ERROR_MESSAGE, failure.error());
        assertTrue(failure.retryable());
    }

    @Test
    void shouldTypeCheckTaskResult() {
        // given
        final TaskResult success = new TaskResult.Success(TASK_ID, SUCCESS_RESULT);
        final TaskResult failure = new TaskResult.Failure(TASK_ID, ERROR_MESSAGE, false);

        // then
        assertTrue(success instanceof TaskResult.Success);
        assertTrue(failure instanceof TaskResult.Failure);
    }

    @Test
    void shouldPatternMatchTaskResult() {
        // given
        final TaskResult result = new TaskResult.Success(TASK_ID, SUCCESS_RESULT);

        // when
        final String message = switch (result) {
            case TaskResult.Success s -> "Success: " + s.result();
            case TaskResult.Failure f -> "Failure: " + f.error();
        };

        // then
        assertTrue(message.startsWith("Success"));
        assertTrue(message.contains(SUCCESS_RESULT.toString()));
    }
}
