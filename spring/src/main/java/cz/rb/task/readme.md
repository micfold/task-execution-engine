# Task Execution Engine Admin API

This document describes the Admin API for the Task Execution Engine (TEE), which provides endpoints for task listing, inspection, and retry.

## Overview

The Task Execution Engine Admin API provides REST endpoints for managing and monitoring tasks. It allows administrators to:

- List and filter tasks
- View task details
- Retry failed tasks
- Get task statistics

## Getting Started

### 1. Enable the Admin API

Add the following to your `application.properties` or `application.yml`:

```properties
task.admin.enabled=true
```

### 2. Configure Dependencies

Ensure you have the necessary Spring modules in your classpath:

```xml
<dependency>
    <groupId>cz.rb</groupId>
    <artifactId>edi-task-execution-engine-spring</artifactId>
    <version>${tee.version}</version>
</dependency>
```

### 3. Import the Configuration

In your application configuration:

```java
@Import(cz.rb.task.config.TaskAdminConfig.class)
public class YourApplicationConfig {
    // Additional configuration
}
```

## API Endpoints

### List Tasks

```
GET /api/v1/admin/tasks
```

**Parameters:**
- `contextId` (optional): Parent context identifier (e.g., clientId, onboardingId)
- `status` (optional): Task status (PENDING, IN_PROGRESS, COMPLETED, FAILED, DEAD_LETTER)
- `type` (optional): Task type
- `startDate` (optional): ISO date-time for filtering by creation date (start)
- `endDate` (optional): ISO date-time for filtering by creation date (end)
- `page` (optional): Page number, 0-based (default: 0)
- `size` (optional): Page size (default: 20)
- `sortBy` (optional): Field to sort by (default: createdAt)
- `sortDir` (optional): Sort direction, asc or desc (default: desc)

**Example:**
```
GET /api/v1/admin/tasks?status=FAILED&type=DOCUMENT_PROCESSING&page=0&size=10
```

**Response:**
```json
[
  {
    "taskId": "task-123",
    "type": "DOCUMENT_PROCESSING",
    "status": "FAILED",
    "retryCount": 2,
    "createdAt": "2025-03-20T10:15:30Z",
    "updatedAt": "2025-03-20T10:20:45Z",
    "data": {
      "documentId": "doc-456",
      "operation": "OCR",
      "clientId": "client-789"
    }
  },
  ...
]
```

### Get Task Details

```
GET /api/v1/admin/tasks/{id}
```

**Parameters:**
- `id`: Task ID

**Example:**
```
GET /api/v1/admin/tasks/task-123
```

**Response:**
```json
{
  "taskId": "task-123",
  "type": "DOCUMENT_PROCESSING",
  "status": "FAILED",
  "retryCount": 2,
  "createdAt": "2025-03-20T10:15:30Z",
  "updatedAt": "2025-03-20T10:20:45Z",
  "data": {
    "documentId": "doc-456",
    "operation": "OCR",
    "clientId": "client-789",
    "error": "Connection timeout while processing document"
  }
}
```

### Retry Task

```
POST /api/v1/admin/tasks/{id}/retry
```

**Parameters:**
- `id`: Task ID

**Example:**
```
POST /api/v1/admin/tasks/task-123/retry
```

**Response:**
```json
{
  "taskId": "task-123",
  "success": true,
  "message": "Task retry successful"
}
```

### Get Task Counts by Status

```
GET /api/v1/admin/tasks/count
```

**Parameters:**
- `contextId` (optional): Parent context identifier
- `type` (optional): Task type filter

**Example:**
```
GET /api/v1/admin/tasks/count?type=DOCUMENT_PROCESSING
```

**Response:**
```json
{
  "PENDING": 5,
  "IN_PROGRESS": 3,
  "COMPLETED": 42,
  "FAILED": 7,
  "DEAD_LETTER": 1
}
```

## Actuator Endpoint

If Spring Boot Actuator is enabled, you can also access task information via the `tasks` actuator endpoint:

```
GET /actuator/tasks
```

**Response:**
```json
{
  "statusCounts": {
    "PENDING": 5,
    "IN_PROGRESS": 3,
    "COMPLETED": 42,
    "FAILED": 7,
    "DEAD_LETTER": 1
  },
  "totalTasks": 58,
  "health": {
    "status": "HEALTHY",
    "failureRate": "13.79%"
  }
}
```

## Extending with Service-Specific Endpoints

You can create service-specific controllers that utilize the `TaskAdminService`:

```java
@RestController
@RequestMapping("/api/service/admin")
public class ServiceAdminController {
    private final TaskAdminService taskAdminService;
    
    // Use taskAdminService to implement custom endpoints
}
```

## Configuration Properties

| Property | Description | Default |
|----------|-------------|---------|
| `task.admin.enabled` | Enables/disables the admin API | `false` |
| `task.admin.use-functional-endpoints` | Use functional endpoints instead of controllers | `false` |
| `task.admin.context-mappings.*` | Maps context names to data fields | - |
| `management.endpoint.tasks.enabled` | Enables/disables the tasks actuator endpoint | `true` |

## Security Considerations

The Admin API does not include built-in security. It should be secured using standard Spring Security mechanisms:

```java
@Configuration
public class AdminSecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
            .antMatchers("/api/v1/admin/**").hasRole("ADMIN")
            // Other configuration
    }
}
```