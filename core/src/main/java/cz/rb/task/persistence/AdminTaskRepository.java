package cz.rb.task.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Extension of TaskRepository with additional query methods for admin operations.
 * This interface adds methods needed specifically for the admin API.
 *
 * @author micfold
 */
@Repository
public interface AdminTaskRepository extends TaskRepository {

    /**
     * Finds tasks by type with custom field filtering.
     *
     * @param type The task type
     * @param fieldName Field name to filter on
     * @param fieldValue Field value to match
     * @return Flux of matching tasks
     */
    @Query("SELECT * FROM tasks WHERE type = :type AND data ->> :fieldName = :fieldValue")
    Flux<TaskEntity> findByTypeAndField(String type, String fieldName, String fieldValue);

    /**
     * Finds tasks based on JSON field values.
     * Useful for filtering by context IDs stored in the data field.
     *
     * @param fieldPath JSON field path (e.g., 'clientId', 'metadata.source')
     * @param value Field value to match
     * @return Flux of matching tasks
     */
    @Query("SELECT * FROM tasks WHERE data ->> :fieldPath = :value")
    Flux<TaskEntity> findByJsonField(String fieldPath, String value);

    /**
     * Finds tasks with full-text search in data field.
     * Useful for searching task data content.
     * Note: This works with PostgreSQL's full-text search capabilities.
     *
     * @param searchTerms Search terms
     * @param pageable Pagination information
     * @return Flux of matching tasks
     */
    @Query("SELECT * FROM tasks WHERE " +
            "to_tsvector('english', COALESCE(data::text, '')) @@ to_tsquery('english', :searchTerms) " +
            "ORDER BY ?#{#pageable.sort} LIMIT ?#{#pageable.pageSize} OFFSET ?#{#pageable.offset}")
    Flux<TaskEntity> searchInTaskData(String searchTerms, Pageable pageable);
}