package cz.rb.task.persistence;

import cz.rb.task.model.Task;
import cz.rb.task.model.TaskStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        @Column("data") String dataRaw,
        @Column("retry_count") Integer retryCount,
        @Column("created_at") Instant createdAt,
        @Column("updated_at") Instant updatedAt
) {
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Constructs TaskEntity with Map data
     */
    public TaskEntity(String taskId, String type, TaskStatus status, Map<String, Object> data,
                      Integer retryCount, Instant createdAt, Instant updatedAt) {
        this(taskId,
                type,
                status,
                mapToJson(data),
                retryCount,
                createdAt,
                updatedAt);
    }


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
                mapToJson(task.data()),
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
                .data(jsonToMap(dataRaw))
                .retryCount(retryCount)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    /**
     * Gets the data as a Map
     */
    public Map<String, Object> data() {
        return jsonToMap(dataRaw);
    }

    /**
     * Helper method to convert Map to JSON string
     */
    private static String mapToJson(Map<String, Object> map) {
        try {
            return mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting Map to JSON", e);
        }
    }

    /**
     * Helper method to convert JSON string to Map
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> jsonToMap(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            return mapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting JSON to Map", e);
        }
    }
}
