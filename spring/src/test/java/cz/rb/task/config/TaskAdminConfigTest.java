package cz.rb.task.config;

import cz.rb.task.engine.DefaultTaskExecutionEngine;
import cz.rb.task.engine.TaskHandlerRegistry;
import cz.rb.task.persistence.AdminTaskRepository;
import cz.rb.task.service.TaskAdminService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for TaskAdminConfig configuration class.
 * Verifies that beans are correctly created based on config properties.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TaskAdminConfig.class})
@TestPropertySource(properties = {
        "task.admin.enabled=true",
        "task.admin.use-functional-endpoints=true",
        "task.admin.context-mappings.client=clientId",
        "task.admin.context-mappings.onboarding=onboardingId"
})
@ActiveProfiles("test")
class TaskAdminConfigTest {

    @MockBean
    private AdminTaskRepository adminTaskRepository;

    @MockBean
    private TaskHandlerRegistry taskHandlerRegistry;

    @MockBean
    private DefaultTaskExecutionEngine executionEngine;

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("Should create TaskAdminService bean when admin is enabled")
    void shouldCreateTaskAdminService() {
        // When / Then
        assertThat(context.containsBean("taskAdminService")).isTrue();
        assertThat(context.getBean("taskAdminService")).isInstanceOf(TaskAdminService.class);
    }

    @Test
    @DisplayName("Should create functional endpoints when configured")
    void shouldCreateFunctionalEndpoints() {
        // When / Then
        assertThat(context.containsBean("taskAdminRoutes")).isTrue();
        assertThat(context.getBean("taskAdminRoutes")).isInstanceOf(RouterFunction.class);
    }

}

/**
 * Tests for TaskAdminConfig when admin is disabled.
 * Verifies that beans are not created when disabled.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TaskAdminConfig.class})
@TestPropertySource(properties = {
        "task.admin.enabled=false"
})
@ActiveProfiles("test")
class TaskAdminConfigDisabledTest {

    @MockBean
    private AdminTaskRepository adminTaskRepository;

    @MockBean
    private TaskHandlerRegistry taskHandlerRegistry;

    @MockBean
    private DefaultTaskExecutionEngine executionEngine;

    @Autowired
    private ApplicationContext context;


    @Test
    @DisplayName("Should not create TaskAdminService when admin is disabled")
    void shouldNotCreateTaskAdminService() {
        // When / Then
        assertThat(context.containsBean("taskAdminService")).isFalse();
    }

    @Test
    @DisplayName("Should not create functional endpoints when admin is disabled")
    void shouldNotCreateFunctionalEndpoints() {
        // When / Then
        assertThat(context.containsBean("taskAdminRoutes")).isFalse();
    }
}