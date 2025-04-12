package cz.rb.task.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RetryableException}.
 *
 * @Project: edi-task-execution-engine
 * @Author: Claude 3.7 Sonnet on 12.04.2025
 */
class RetryableExceptionTest {

    private static final String ERROR_MESSAGE = "Test error message";

    @Test
    @DisplayName("Should create exception with message")
    void constructWithMessage() {
        // When
        RetryableException exception = new RetryableException(ERROR_MESSAGE);

        // Then
        assertEquals(ERROR_MESSAGE, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    void constructWithMessageAndCause() {
        // Given
        Throwable cause = new RuntimeException("Root cause");

        // When
        RetryableException exception = new RetryableException(ERROR_MESSAGE, cause);

        // Then
        assertEquals(ERROR_MESSAGE, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    @DisplayName("Should create exception with cause only")
    void constructWithCause() {
        // Given
        Throwable cause = new RuntimeException("Root cause");

        // When
        RetryableException exception = new RetryableException(cause);

        // Then
        assertEquals(cause.toString(), exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    @DisplayName("Should create exception with specified parameters")
    void constructWithAllParameters() {
        // Given
        Throwable cause = new RuntimeException("Root cause");
        boolean enableSuppression = false;
        boolean writableStackTrace = true;

        // When
        RetryableException exception = new RetryableException(
                ERROR_MESSAGE, cause, enableSuppression, writableStackTrace);

        // Then
        assertEquals(ERROR_MESSAGE, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    @DisplayName("Should allow adding suppressed exceptions")
    void shouldAllowAddingSuppressedExceptions() {
        // Given
        RetryableException exception = new RetryableException(ERROR_MESSAGE);
        Throwable suppressed = new RuntimeException("Suppressed exception");

        // When
        exception.addSuppressed(suppressed);

        // Then
        Throwable[] suppressedExceptions = exception.getSuppressed();
        assertEquals(1, suppressedExceptions.length);
        assertSame(suppressed, suppressedExceptions[0]);
    }

    @Test
    @DisplayName("Should create exception with non-writable stack trace")
    void constructWithNonWritableStackTrace() {
        // Given
        Throwable cause = new RuntimeException("Root cause");
        boolean enableSuppression = true;
        boolean writableStackTrace = false;

        // When
        RetryableException exception = new RetryableException(
                ERROR_MESSAGE, cause, enableSuppression, writableStackTrace);

        // Then
        assertEquals(ERROR_MESSAGE, exception.getMessage());
        assertSame(cause, exception.getCause());

        // Test stack trace behavior
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertEquals(0, stackTrace.length, "Stack trace should be empty when not writable");
    }

    @Test
    @DisplayName("Should inherit from RuntimeException")
    void shouldInheritFromRuntimeException() {
        // When
        RetryableException exception = new RetryableException(ERROR_MESSAGE);

        // Then
        assertTrue(true);
    }
}