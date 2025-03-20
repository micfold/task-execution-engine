package cz.rb.task.persistence;

import cz.rb.task.model.Task;
import cz.rb.task.model.TaskStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Map;

/**
 * Database entity for task persistence.
 * Maps task domain objects to database records.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 22.02.2025
 */
@Table("tasks")
public record TaskEntity(
        @Id @Column("task_id") String taskId,
        @Column("type") String type,
        @Column("status") TaskStatus status,
        @Column("data") Map<String, Object> data,
        @Column("retry_count") Integer retryCount,
        @Column("created_at") Instant createdAt,
        @Column("updated_at") Instant updatedAt
) {
    /**
     * Creates a TaskEntity from a domain Task object.
     *
     * @param task The domain task object
     * @return A new TaskEntity
     */
    public static TaskEntity fromDomain(Task task) {
        return new TaskEntity(
                task.taskId(),
                task.type(),
                task.status(),
                task.data(),
                task.retryCount(),
                task.createdAt(),
                task.updatedAt()
        );
    }

    /**
     * Converts this entity to a domain Task object.
     *
     * @return A new Task domain object
     */
    public Task toDomain() {
        return Task.builder()
                .taskId(taskId)
                .type(type)
                .status(status)
                .data(data)
                .retryCount(retryCount)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    /**
     * Creates a new TaskEntity with incremented retry count.
     *
     * @return A new TaskEntity instance
     */
    public TaskEntity incrementRetry() {
        return new TaskEntity(
                taskId,
                type,
                status,
                data,
                retryCount + 1,
                createdAt,
                Instant.now()
        );
    }
}
