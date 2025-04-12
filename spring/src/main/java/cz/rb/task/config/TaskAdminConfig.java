package cz.rb.task.config;

import cz.rb.task.engine.DefaultTaskExecutionEngine;
import cz.rb.task.engine.TaskHandlerRegistry;
import cz.rb.task.persistence.AdminTaskRepository;
import cz.rb.task.service.TaskAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Configuration for Task Execution Engine admin API.
 * Sets up the admin endpoints and services.
 *
 * @author micfold
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
        name = "task.admin.enabled",
        havingValue = "true",
        matchIfMissing = false)
public class TaskAdminConfig {

    /**
     * Creates the task admin service bean.
     * This service provides task management capabilities needed by the admin endpoints.
     *
     * @param repository The task repository
     * @param registry The task handler registry
     * @param executionEngine The task execution engine
     * @return A configured TaskAdminService
     */
    @Bean
    @Description("Service providing task administration capabilities")
    public TaskAdminService taskAdminService(
            AdminTaskRepository repository,
            TaskHandlerRegistry registry,
            DefaultTaskExecutionEngine executionEngine
    ) {
        log.info("Configuring TaskAdminService");
        return new TaskAdminService(repository, registry, executionEngine);
    }

    /**
     * Optional WebFlux endpoint configuration for admin API.
     * Can be used instead of @RestController if preferred.
     */
    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnProperty(name = "task.admin.use-functional-endpoints", havingValue = "true", matchIfMissing = false)
    static class FunctionalEndpointConfig {

        /**
         * Creates router functions for the admin API endpoints.
         *
         * @param adminService The task admin service
         * @return Router function for admin endpoints
         */
        @Bean
        @Description("Router function for task admin API endpoints")
        public RouterFunction<ServerResponse>
        taskAdminRoutes(TaskAdminService adminService) {
            log.info("Configuring functional endpoints for task admin API");

            return RouterFunctions
                    .route()
                    .GET("/api/v1/admin/tasks", request -> {
                        // Extract query parameters
                        final String contextId = request.queryParam("contextId").orElse(null);
                        cz.rb.task.model.TaskStatus status = request.queryParam("status")
                                .map(cz.rb.task.model.TaskStatus::valueOf)
                                .orElse(null);
                        // Add other parameters...

                        return ServerResponse.ok()
                                .body(adminService.findTasks(  contextId,
                                                status,
                                                null,
                                                null,
                                                null,
                                                0,
                                                20,
                                                "createdAt",
                                                "desc"),
                                        cz.rb.task.model.Task.class);
                    })
                    .GET("/api/v1/admin/tasks/{id}", request -> {
                        String id = request.pathVariable("id");
                        return adminService.getTaskById(id)
                                .flatMap(task -> ServerResponse
                                        .ok().bodyValue(task))
                                .switchIfEmpty(ServerResponse
                                        .notFound().build());
                    })
                    .POST("/api/v1/admin/tasks/{id}/retry", request -> {
                        String id = request.pathVariable("id");
                        return adminService.retryTask(id)
                                .flatMap(result -> ServerResponse
                                        .ok().bodyValue(result))
                                .switchIfEmpty(ServerResponse
                                        .notFound().build());
                    })
                    .build();
        }
    }
}