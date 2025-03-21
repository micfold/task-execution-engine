package cz.rb.task.api;

import cz.rb.task.model.Task;
import cz.rb.task.model.TaskResult;
import cz.rb.task.model.TaskStatus;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for TaskHandler interface.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 22.02.2025
 */
class TaskHandlerContractTest {

    private final TestTaskHandler handler = new TestTaskHandler();

    @Test
    void shouldDefineTaskType() {
        // When
        final String taskType = handler.getTaskType();

        // Then
        assertNotNull(taskType);
        assertFalse(taskType.isBlank());
    }

    @Test
    void shouldHandleTaskExecution() {
        // given
        final Task task = Task.builder()
                .taskId("test-task")
                .type(handler.getTaskType())
                .data(Map.of("test", "data"))
                .status(TaskStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // when
        Mono<TaskResult> result = handler.execute(task);

        // then
        StepVerifier.create(result)
                .expectNextMatches(taskResult ->
                        taskResult instanceof TaskResult.Success &&
                                taskResult.taskId().equals(task.taskId()))
                .verifyComplete();
    }

    /**
     * Test implementation of TaskHandler for contract verification.
     */
    private static class TestTaskHandler implements TaskHandler {
        @Override
        public String getTaskType() {
            return "TEST_TASK";
        }

        @Override
        public Mono<TaskResult> execute(final Task task) {
            return Mono.just(new TaskResult.Success(
                    task.taskId(),
                    Map.of("executed", true)
            ));
        }
    }

}
