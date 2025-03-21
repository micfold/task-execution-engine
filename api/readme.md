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
The TaskHandler is the key interface that connects your business logic to the TEE framework. 
It's designed to be implemented by developers in their own services, not by the framework itself.
```java
public interface TaskHandler {
   // Identifies which task type this handler can process
   String getTaskType();

   // Contains the actual business logic for executing the task
   Mono<TaskResult> execute(final Task task);
}
```
### How it works? (Decentralized Approach)
Since TEE ought to be a framework without a central database or running server, here's how it works:

1. **Each service manages its own tasks**: Services using TEE would have their own database tables for tasks, 
created using the schema management tools provided by TEE.
2. **Local task execution**: When a service wants to execute a task asynchronously:
- It creates a Task object
- Persists it in its own database
- Uses the TEE execution engine to process it
3. **Handler registration**: Each service registers its own TaskHandler with the local TaskHandlerRegistry:
```java
@PostConstruct
public void registerHandlers() {
    handlerRegistry.registerHandler(new DocumentProcessingHandler());
    handlerRegistry.registerHandler(new ContractRegistrationHanlder());
    handlerRegistry.registerHandler(new PaymentProcessingHandler());
}
```
4. **Execution flow**: When the service needs to execute a task:
- It create a Task
- The DefaultTaskExecutionEngine finds the appropriate handler from the registry
- It calls the handler's execute method
- The engine handles retries, state management, and error handling

### Example: Decentralized Implementation
```java
@Service
@RequiredArgsConstructor
public class ClientOnboardingService {
    private final DefaultTaskExecutionEngine executionEngine;
    private final TaskHandlerRegistry handlerRegistry;
    
    // All handlers would be local to this service
    private final PortfolioCreationHandler portfolioHandler;
    private final ContractRegistrationHandler contractHandler;
    private final MultichannelEnablementHandler channelHandler;
    
    @PostConstruct
    public void registerHandlers() {
        // Register handlers with the local registry
        handlerRegistry.registerHandler(portfolioHandler);
        handlerRegistry.registerHandler(contractHandler);
        handlerRegistry.registerHandler(channelHandler);
    }
    
    public Mono<String> startOnboardingProcess(final String clientId, Map<String, Object> contractDetails) {
        // Create tasks and submit them to the local execution engine
        final Task portfolioTask = createPortfolioTask(clientId);
        
        // The execution engine persists tasks in this service's database
        // and processes them using the locally registered handlers
        return executionEngine.executeTask(portfolioTask, portfolioHandler)
                .flatMap(result -> {
                    if (result instanceof TaskResult.Success) {
                        // Continue with next task
                        return executionEngine.executeTask(
                                createContractTask(clientId, contractDetails),
                                contractHandler
                        );
                    }
                    return Mono.just(result);
                })
                .flatMap(result -> {
                    if (result instanceof TaskResult.Success) {
                        // Continue with final task
                        return executionEngine.executeTask(
                                createMultichannelTask(clientId),
                                channelHandler
                        );
                    }
                    return Mono.just(result);
                })
                .map(result -> clientId);
    }
    
    // Helper methods to create the tasks
    private Task createPortfolioTask(final String clientId) {
        return TaskBuilder.forType("PORTFOLIO_CREATION")
                .withData("clientId", clientId)
                .build();
    }
    
    // Other helper methods...
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