package cz.rb.task.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for custom exceptions.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 22.02.2025
 */
class ExceptionTest {
    private static final String TASK_ID = "test-task";
    private static final String ERROR_MESSAGE = "Test error";

    @Test
    void shouldCreateTaskExecutionException() {
        // when
        final TaskExecutionException exception = new TaskExecutionException(ERROR_MESSAGE);

        // then
        assertEquals(ERROR_MESSAGE, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateTaskExecutionExceptionWithCause() {
        // given
        final RuntimeException cause = new RuntimeException("Root cause");

        // when
        final TaskExecutionException exception = new TaskExecutionException(ERROR_MESSAGE, cause);

        // then
        assertEquals(ERROR_MESSAGE, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void shouldCreateTaskNotFoundException() {
        // when
        final TaskNotFoundException exception = new TaskNotFoundException(TASK_ID);

        // then
        assertTrue(exception.getMessage().contains(TASK_ID));
    }


}
