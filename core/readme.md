# Task Execution Engine Core Module

This module implements a reactive task execution engine with retry policies, dead letter queues, and event publishing capabilities.

## Core Features
- Non-blocking reactive execution using Project Reactor
- Configurable retry strategy with exponential backoff
- Dead letter queue (DLQ) handling
- Event publishing for task lifecycle events
- Task handler registry with dynamic registration

## Key Components

### DefaultTaskExecutionEngine
```java
@Service
public class DefaultTaskExecutionEngine {
    public Mono<TaskResult> executeTask(Task task, TaskHandler handler);
}
```

Features:
- Task status management
- Retry handling via RetryStrategy
- Event publishing
- Error handling and DLQ integration

### TaskHandlerRegistry
```java
@Component
public class TaskHandlerRegistry {
    public void registerHandler(TaskHandler handler);
    public Mono<TaskHandler> getHandler(String taskType);
    public boolean removeHandler(String taskType);
    public boolean hasHandler(String taskType);
    public int getHandlerCount();
    public void clearHandlers();
}
```

### RetryStrategy
```java
@Component
public class RetryStrategy {
    public Mono<TaskResult> executeWithRetry(
        Task task,
        Function<Task, Mono<TaskResult>> execution
    );
}
```

Configuration properties:
- task.execution.max-retries (default: 3)
- task.execution.initial-delay (default: 1s)
- task.execution.max-delay (default: 60s)

### Event Publishing
```java
@Component
public class TaskEventPublisher {
    public void publishEvent(TaskEvent event);
}
```

Event types:
- TASK_CREATED
- TASK_STARTED
- TASK_COMPLETED
- TASK_FAILED
- MOVED_TO_DLQ

### Database Schema
```sql
CREATE TABLE tasks (
    task_id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    data JSONB,
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_type ON tasks(type);
```

## Usage Example

```java
@Service
public class DocumentProcessingService {
    private final DefaultTaskExecutionEngine engine;
    private final TaskHandler documentHandler;

    public Mono<TaskResult> processDocument(String documentId) {
        Task task = Task.builder()
            .taskId(UUID.randomUUID().toString())
            .type("DOCUMENT_PROCESSING")
            .data(Map.of("documentId", documentId))
            .status(TaskStatus.PENDING)
            .retryCount(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        return engine.executeTask(task, documentHandler);
    }
}
```

## Error Handling

1. Retryable Errors
- Network timeouts
- Temporary service unavailability
- Transient database errors

2. Non-Retryable Errors
- Invalid task data
- Business rule violations
- Permanent failures

3. Dead Letter Queue
- Final destination for non-retryable failures
- Stores complete error context
- Enables manual intervention

## Configuration

```yaml
task:
  execution:
    max-retries: 3
    initial-delay: 1
    max-delay: 60
  kafka:
    topic:
      events: task-events
      dlq: task-dlq
```

## Support
- Technical Owner: Michael Foldyna
- Created: 22/02/2025