# Task Execution Engine Integration Guide

This guide explains how to integrate the Task Execution Engine (TEE) into your Spring Boot application, with a focus on database schema integration.

## Table of Contents

- [Overview](#overview)
- [Project Structure](#project-structure)
- [Integration Steps](#integration-steps)
- [Example: Integrating with Foo Bar Service](#example-integrating-with-foo-bar-service)
- [Schema Customization](#schema-customization)
- [Manual Schema Initialization](#manual-schema-initialization)
- [Monitoring and Management](#monitoring-and-management)
- [Common Issues and Solutions](#common-issues-and-solutions)

## Overview

The Task Execution Engine (TEE) is a framework that provides robust task execution capabilities for Spring Boot applications. It handles task scheduling, execution, retries, and status tracking through a well-defined API.

One of TEE's key features is its ability to manage its own database schema independently from the host application's schema. This allows for clean integration without schema conflicts.

## Project Structure

The TEE framework consists of four modules:

1. **api**: Core interfaces and domain models
2. **core**: Implementation of the core execution engine
3. **spring**: Spring-specific integration components including schema management
4. **starter**: Spring Boot starter for auto-configuration

## Integration Steps

### 1. Add Dependencies

Add the TEE starter dependency to your project:

```xml
<dependency>
    <groupId>cz.rb</groupId>
    <artifactId>edi-task-execution-engine-starter</artifactId>
    <version>${tee.version}</version>
</dependency>
```

### 2. Configure Database Connection

Configure your database connection as usual in your application. TEE will use the same DataSource for its schema:

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/yourdb
    username: youruser
    password: yourpassword
```

### 3. Database Schema Options

Choose one of the following approaches:

#### Option A: Auto-initialization (for development)

Enable auto-initialization in your application.yml:

```yaml
task:
  persistence:
    auto-initialize: true
```

#### Option B: Programmatic Initialization (recommended for production)

Use the `TEESchemaCreationService` to initialize the schema programmatically:

```java
@Autowired
private TEESchemaCreationService schemaService;

@PostConstruct
public void initializeSchema() {
    schemaService.createSchema(
        SchemaOptions.builder()
            .tablePrefix("my_app_")
            .enableAuditEvents(true)
            .build()
    );
}
```

## Example: Integrating with Foo Bar Service

Let's walk through integrating TEE with a fictional "Foo Bar" service.

### Step 1: Add Dependencies

In the Foo Bar service's `pom.xml`:

```xml
<dependency>
    <groupId>cz.rb</groupId>
    <artifactId>edi-task-execution-engine-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Step 2: Configure Database Connection

In `application.yml`:

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://db.foobar.com:5432/foobar_db
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    
# Disable auto-initialization for production
task:
  persistence:
    auto-initialize: false
```

### Step 3: Create Schema Initializer

Create a class to initialize the TEE schema:

```java
package com.foobar.config;

import cz.rb.task.spring.schema.SchemaOptions;
import cz.rb.task.spring.schema.TEESchemaCreationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TEEConfiguration {

    private final TEESchemaCreationService schemaService;
    
    @PostConstruct
    public void initializeSchema() {
        log.info("Initializing Task Execution Engine schema");
        boolean success = schemaService.createSchema(
            SchemaOptions.builder()
                .tablePrefix("foobar_")
                .schemaName("tasks")
                .enableAuditEvents(true)
                .build()
        );
        
        if (success) {
            log.info("TEE schema initialized successfully");
        } else {
            log.error("Failed to initialize TEE schema");
        }
    }
}
```

### Step 4: Creating Task Handlers

Now you can use the Task Execution Engine in your service:

```java
package com.foobar.service;

import cz.rb.task.api.TaskService;
import cz.rb.task.model.Task;
import cz.rb.task.model.TaskResult;
import cz.rb.task.model.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final TaskService taskService;
    
    public Mono<String> processDocument(String documentId) {
        Task task = Task.builder()
                .taskId(UUID.randomUUID().toString())
                .type("DOCUMENT_PROCESSING")
                .data(Map.of("documentId", documentId))
                .status(TaskStatus.PENDING)
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
                
        return taskService.submitTask(task)
                .map(result -> {
                    if (result instanceof TaskResult.Success) {
                        return "Document processing started successfully";
                    } else {
                        return "Failed to start document processing";
                    }
                });
    }
}
```

## Schema Customization

You can customize the TEE schema using the `SchemaOptions` builder:

```java
SchemaOptions options = SchemaOptions.builder()
    .schemaName("custom_schema")      // Database schema name
    .tablePrefix("app_")              // Prefix for all table names
    .tasksTableName("tasks")          // Base name for tasks table
    .eventsTableName("task_events")   // Base name for events table
    .enableAuditEvents(true)          // Whether to create the events table
    .enableConstraints(true)          // Whether to add CHECK constraints
    .dropExistingTables(false)        // Whether to drop existing tables
    .build();
```

This would create tables:
- `custom_schema.app_tasks`
- `custom_schema.app_task_events`

## Manual Schema Initialization

For complex scenarios such as CI/CD pipelines or applications with managed schema migrations, you can generate SQL scripts:

```java
@Autowired
private TEESchemaCreationService schemaService;

public String generateSchemaScript() {
    return schemaService.generateScript(
        SchemaOptions.builder()
            .tablePrefix("my_app_")
            .build()
    );
}
```

Then use your preferred migration tool to apply the script.

## Monitoring and Management

TEE provides several endpoints for monitoring and management:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,tasks
```

The `/tasks` endpoint provides information about task execution and status.

## Common Issues and Solutions

### Table Already Exists

If you receive an error like "relation already exists", you have two options:

1. Use a different schema name or table prefix
2. Set `dropExistingTables(true)` in your options (caution: this will delete existing data)

### Missing Handler Registration

If tasks are being created but not executed, ensure you've registered task handlers:

```java
@Component
public class AppStartup {
    @Autowired
    private TaskHandlerRegistry handlerRegistry;
    
    @Autowired
    private DocumentProcessingHandler documentHandler;
    
    @PostConstruct
    public void registerHandlers() {
        handlerRegistry.registerHandler(documentHandler);
    }
}
```

### Schema Permission Issues

Ensure your database user has privileges to create schemas and tables. For PostgreSQL:

```sql
GRANT CREATE ON DATABASE your_database TO your_user;
GRANT USAGE, CREATE ON SCHEMA public TO your_user;
```

For enhanced security in production, create a dedicated schema:

```sql
CREATE SCHEMA tasks;
GRANT USAGE, CREATE ON SCHEMA tasks TO your_user;
```

Then configure TEE to use this schema:

```java
SchemaOptions.builder().schemaName("tasks").build();
```