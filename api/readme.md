# Task Execution Engine API Module

This module defines the core interfaces and domain models for the Task Execution Engine. It serves as the contract between task producers, task handlers, and the execution engine itself.

## Overview

The API module provides a reactive interface for submitting and monitoring tasks in a distributed system. It is designed to be:

- **Non-blocking**: All operations return Reactor types (Mono/Flux)
- **Type-safe**: Uses sealed interfaces and records for compile-time safety
- **Extensible**: Easy to implement new task types via the TaskHandler interface
- **Immutable**: Uses records to ensure thread safety

## Core Components

### Interfaces

#### TaskHandler
Interface for implementing domain-specific task handlers:
```java
public interface TaskHandler {
    String getTaskType();
    Mono<TaskResult> execute(Task task);
}
```

### Domain Models

#### Task
Represents a unit of work to be executed:
```java
public record Task(
    String taskId,
    String type,
    Map<String, Object> data,
    TaskStatus status,
    String handlerUrl,
    Integer retryCount,
    Instant createdAt,
    Instant updatedAt
) { }
```

#### TaskResult
Sealed interface representing execution outcomes:
```java
public sealed interface TaskResult {
    record Success(String taskId, Map<String, Object> result) implements TaskResult {}
    record Failure(String taskId, String error, boolean retryable) implements TaskResult {}
}
```

## Implementation Guidelines

### Task Handler Implementation

Keep handlers focused and follow these principles:
- One responsibility per handler
- Clear error handling
- Proper retry configuration
- Comprehensive testing

Example:
```java
@Component
public class DocumentProcessingHandler implements TaskHandler {
    @Override
    public String getTaskType() {
        return "DOCUMENT_PROCESSING";
    }

    @Override
    public Mono<TaskResult> execute(Task task) {
        return processDocument(task.data())
            .map(result -> new TaskResult.Success(task.taskId(), result))
            .onErrorResume(error -> Mono.just(
                new TaskResult.Failure(task.taskId(), error.getMessage(), true)
            ));
    }
}
```

### Error Handling

The module provides two main exception types:

1. `TaskExecutionException`
    - Used for task execution failures
    - Contains error details
    - Supports retry indication

2. `TaskNotFoundException`
    - Used when tasks cannot be found
    - Includes task ID reference

## Testing

All components must be thoroughly tested. Key test areas:

1. **Domain Models**
    - Builder pattern validation
    - Immutability verification
    - State transitions

2. **Contract Tests**
    - Interface compliance
    - Error handling
    - Reactive stream behavior

Example test:
```java
@Test
void shouldHandleTaskExecution() {
    Task task = Task.builder()
        .taskId("test-task")
        .type("TEST_TYPE")
        .data(Map.of("key", "value"))
        .status(TaskStatus.PENDING)
        .build();

    StepVerifier.create(handler.execute(task))
        .expectNextMatches(result -> result instanceof TaskResult.Success)
        .verifyComplete();
}
```

## Contributing

When adding or modifying components:

1. Maintain backward compatibility
2. Add comprehensive tests
3. Update relevant documentation
4. Follow existing patterns

## Support

For issues and questions:
- Technical Owner: Michael Foldyna
- Created: 22/02/2025