# EDI Task Execution Engine

A distributed task execution engine designed for handling asynchronous processing across RB EDI services. Built with reactive principles and Spring Boot.

## Project Overview

The Task Execution Engine provides a robust framework for:
- Asynchronous task processing
- Distributed task handling
- Retry management
- Dead Letter Queue (DLQ) processing
- Task status monitoring

## Architecture

The project is structured into modules:

```
edi-task-execution-engine/
├── api/          # Core interfaces and domain models
├── core/         # Core engine implementation
├── spring/       # Spring Framework integration
└── starter/      # Spring Boot starter
```

### Modules

- **api**: Core interfaces and models defining the task execution contract
- **core**: Implementation of task execution engine and handler registry
- **spring**: Spring-specific implementations and configurations
- **starter**: Spring Boot starter for easy integration

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.8+
- PostgreSQL 14+
- Kafka 3.x

### Building

```bash
# Build all modules
mvn clean install

# Skip tests
mvn clean install -DskipTests
```

### Integration

Add the starter to your Spring Boot project:

```xml
<dependency>
    <groupId>cz.rb</groupId>
    <artifactId>edi-task-execution-starter</artifactId>
    <version>${edi-task-engine.version}</version>
</dependency>
```

### Configuration

Application properties:
```yaml
edi:
  task:
    engine:
      max-retries: 3
      retry-delay: PT1S
      dlq-topic: your-service.dlq
      task-events-topic: your-service.events
    kafka:
      bootstrap-servers: localhost:9092
    database:
      url: r2dbc:postgresql://localhost:5432/taskdb
```

## Usage

### Implementing Task Handlers

1. Create a handler implementation:
```java
@Component
public class YourTaskHandler implements TaskHandler {
    @Override
    public String getTaskType() {
        return "YOUR_TASK_TYPE";
    }

    @Override
    public Mono<TaskResult> execute(Task task) {
        // Your task implementation
    }
}
```

2. Submit tasks:
```java
@Service
public class YourService {
    private final TaskService taskService;

    public Mono<TaskResult> submitTask(String data) {
        Task task = Task.builder()
            .taskId(UUID.randomUUID().toString())
            .type("YOUR_TASK_TYPE")
            .data(Map.of("key", data))
            .build();

        return taskService.submitTask(task);
    }
}
```

## Development

### Building from Source

```bash
git clone https://github.com/rb/edi-task-execution-engine.git
cd edi-task-execution-engine
mvn clean install
```

### Running Tests

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify
```

### Code Style

This project uses:
- Google Java Style Guide
- Checkstyle for style enforcement
- SonarQube for code quality

## Database Schema

Key tables:
- `tasks`: Main task storage
- `task_events`: Task execution events
- `task_handlers`: Registered handler information

## Monitoring

The engine exposes the following metrics:
- Task execution counts and rates
- Processing times
- Retry statistics
- DLQ metrics

Metrics are available via:
- Prometheus endpoints
- JMX
- Actuator endpoints

## Troubleshooting

Common issues:

1. Task Timeout
```yaml
edi.task.engine.execution-timeout: PT30S
```

2. Database Connection
```yaml
edi.task.engine.database.pool-size: 10
```

3. Kafka Configuration
```yaml
edi.task.engine.kafka.retry-backoff: PT1S
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit changes
4. Create a pull request

### Pull Request Process

1. Update documentation
2. Add tests
3. Update CHANGELOG.md
4. Get approval from maintainers

## Release Process

1. Update version in pom.xml files
2. Run full test suite
3. Create release branch
4. Deploy to artifactory
5. Tag release

## License

Copyright (c) 2025 Raiffeisenbank a.s.
All rights reserved.# TaskExecutionEngine
