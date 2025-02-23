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
 * Provides reactive database operations for tasks.
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
    Flux<TaskEntity> findByStatus(TaskStatus status);

    /**
     * Finds tasks by type and status.
     *
     * @param type The task type
     * @param status The task status
     * @return Flux of matching tasks
     */
    Flux<TaskEntity> findByTypeAndStatus(String type, TaskStatus status);

    /**
     * Counts tasks by type and status.
     *
     * @param type The task type
     * @param status The task status
     * @return Mono with the count
     */
    Mono<Long> countByTypeAndStatus(String type, TaskStatus status);

    /**
     * Finds failed tasks eligible for retry.
     *
     * @param maxRetries Maximum retry count
     * @return Flux of retryable tasks
     */
    @Query("SELECT * FROM tasks WHERE status = 'FAILED' AND retry_count < :maxRetries")
    Flux<TaskEntity> findFailedTasksForRetry(int maxRetries);

    /**
     * Finds tasks that have been stuck in progress for too long.
     *
     * @param threshold Time threshold for considering a task stuck
     * @return Flux of stuck tasks
     */
    @Query("SELECT * FROM tasks WHERE status = 'IN_PROGRESS' AND updated_at < :threshold")
    Flux<TaskEntity> findStuckTasks(Instant threshold);

    /**
     * Finds tasks by type with pagination.
     *
     * @param type The task type
     * @param pageable Pagination information
     * @return Flux of tasks
     */
    Flux<TaskEntity> findByType(String type, Pageable pageable);

    /**
     * Updates task status by ID.
     *
     * @param taskId Task identifier
     * @param status New status
     * @return Number of rows affected
     */
    @Query("UPDATE tasks SET status = :status, updated_at = NOW() WHERE task_id = :taskId")
    Mono<Integer> updateTaskStatus(String taskId, TaskStatus status);

    /**
     * Increments retry count for a task.
     *
     * @param taskId Task identifier
     * @return Number of rows affected
     */
    @Query("UPDATE tasks SET retry_count = retry_count + 1, updated_at = NOW() WHERE task_id = :taskId")
    Mono<Integer> incrementRetryCount(String taskId);

    /**
     * Deletes completed tasks older than the specified time.
     *
     * @param threshold Time threshold for deletion
     * @return Number of tasks deleted
     */
    @Query("DELETE FROM tasks WHERE status = 'COMPLETED' AND updated_at < :threshold")
    Mono<Long> deleteCompletedTasksOlderThan(Instant threshold);
}