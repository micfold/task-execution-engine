package cz.rb.task.controller;

import cz.rb.task.model.Task;
import cz.rb.task.model.TaskResult;
import cz.rb.task.model.TaskStatus;
import cz.rb.task.service.TaskAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * REST controller for task administration.
 * Provides endpoints for task listing, inspection, and retry capabilities.
 *
 * @author micfold
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/tasks")
@RequiredArgsConstructor
public class TaskAdminController {

    private final TaskAdminService taskAdminService;

    /**
     * Lists and filters tasks with pagination support.
     *
     * @param contextId  Optional parent context identifier (e.g., clientId, onboardingId)
     * @param status     Optional task status filter
     * @param type       Optional task type filter
     * @param startDate  Optional start date filter
     * @param endDate    Optional end date filter
     * @param page       Page number (0-based)
     * @param size       Page size
     * @param sortBy     Field to sort by
     * @param sortDir    Sort direction (asc or desc)
     * @return Flux of matching tasks
     */
    @GetMapping
    public Flux<Task> listTasks(
            @RequestParam(name = "contextId", required = false) String contextId,
            @RequestParam(name = "status", required = false) TaskStatus status,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir
    ) {
        log.info("Listing tasks with filters: contextId={}, status={}, type={}, page={}, size={}",
                contextId, status, type, page, size);

        return taskAdminService.findTasks(contextId, status, type, startDate, endDate, page, size, sortBy, sortDir);
    }

    /**
     * Retrieves detailed information about a specific task.
     *
     * @param id Task ID
     * @return Task details
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Task>> getTaskDetails(@PathVariable(name = "id") String id) {
        log.info("Fetching details for task: {}", id);

        return taskAdminService.getTaskById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnError(e -> log.error("Error fetching task details for {}: {}", id, e.getMessage(), e));
    }

    /**
     * Manually retries a failed task.
     *
     * @param id Task ID to retry
     * @return Result of the retry operation
     */
    @PostMapping("/{id}/retry")
    public Mono<ResponseEntity<Map<String, Object>>> retryTask(@PathVariable(name = "id") String id) {
        log.info("Attempting to retry task: {}", id);

        return taskAdminService.retryTask(id)
                .map(result -> {
                    Map<String, Object> response = Map.of(
                            "taskId", id,
                            "success", result instanceof TaskResult.Success,
                            "message", result instanceof TaskResult.Success
                                    ? "Task retry successful"
                                    : ((TaskResult.Failure) result).error()
                    );
                    return ResponseEntity.ok(response);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnError(e -> log.error("Error retrying task {}: {}", id, e.getMessage(), e));
    }

    /**
     * Gets task count by status.
     *
     * @param contextId Optional parent context identifier
     * @param type Optional task type filter
     * @return Map of status counts
     */
    @GetMapping("/count")
    public Mono<ResponseEntity<Map<TaskStatus, Long>>> getTaskCounts(
            @RequestParam(name = "contextId", required = false) String contextId,
            @RequestParam(name = "type", required = false) String type
    ) {
        log.info("Getting task counts for contextId={}, type={}", contextId, type);

        return taskAdminService.countTasksByStatus(contextId, type)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.ok(Map.of()));
    }
}