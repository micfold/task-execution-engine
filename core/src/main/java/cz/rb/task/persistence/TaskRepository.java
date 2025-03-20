package cz.rb.task.persistence;

import cz.rb.task.model.TaskStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Repository interface for Task entity persistence.
 * Provides reactive database operations for tasks with enhanced query capabilities.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 22.02.2025
 */
@Repository
public interface TaskRepository extends ReactiveCrudRepository<TaskEntity, String> {

    /**
     * Finds tasks by their current status.
     *
     * @param status The status to search for
     * @return Flux of matching tasks
     */
    Flux<TaskEntity> findByStatus(final TaskStatus status);

    /**
     * Finds tasks by their current status with pagination.
     *
     * @param status The status to search for
     * @param pageable Pagination information
     * @return Flux of matching tasks
     */
    Flux<TaskEntity> findByStatus(final TaskStatus status, Pageable pageable);

    /**
     * Counts tasks by their current status.
     *
     * @param status The status to search for
     * @return Mono with the count
     */
    Mono<Long> countByStatus(final TaskStatus status);

    /**
     * Finds tasks by type.
     *
     * @param type The task type
     * @return Flux of matching tasks
     */
    Flux<TaskEntity> findByType(final String type);

    /**
     * Finds tasks by type with pagination.
     *
     * @param type The task type
     * @param pageable Pagination information
     * @return Flux of tasks
     */
    Flux<TaskEntity> findByType(final String type, Pageable pageable);

    /**
     * Counts tasks by type.
     *
     * @param type The task type
     * @return Mono with the count
     */
    Mono<Long> countByType(final String type);

    /**
     * Finds tasks by type and status.
     *
     * @param type The task type
     * @param status The task status
     * @return Flux of matching tasks
     */
    Flux<TaskEntity> findByTypeAndStatus(final String type, TaskStatus status);

    /**
     * Finds tasks by type and status with pagination.
     *
     * @param type The task type
     * @param status The task status
     * @param pageable Pagination information
     * @return Flux of matching tasks
     */
    Flux<TaskEntity> findByTypeAndStatus(final String type, TaskStatus status, Pageable pageable);

    /**
     * Counts tasks by type and status.
     *
     * @param type The task type
     * @param status The task status
     * @return Mono with the count
     */
    Mono<Long> countByTypeAndStatus(final String type, TaskStatus status);

    /**
     * Finds all tasks with pagination.
     *
     * @param pageable Pagination information
     * @return Flux of tasks
     */
    @Query("SELECT * FROM tasks ORDER BY ?#{#pageable.sort} LIMIT ?#{#pageable.pageSize} OFFSET ?#{#pageable.offset}")
    Flux<TaskEntity> findAllWithPagination(final Pageable pageable);

    /**
     * Finds failed tasks eligible for retry.
     *
     * @param maxRetries Maximum retry count
     * @return Flux of retryable tasks
     */
    @Query("SELECT * FROM tasks WHERE status = 'FAILED' AND retry_count < :maxRetries")
    Flux<TaskEntity> findFailedTasksForRetry(final int maxRetries);

    /**
     * Finds tasks that have been stuck in progress for too long.
     *
     * @param threshold Time threshold for considering a task stuck
     * @return Flux of stuck tasks
     */
    @Query("SELECT * FROM tasks WHERE status = 'IN_PROGRESS' AND updated_at < :threshold")
    Flux<TaskEntity> findStuckTasks(final Instant threshold);
}