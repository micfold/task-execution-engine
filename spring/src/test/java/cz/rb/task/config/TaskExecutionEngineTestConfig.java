package cz.rb.task.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Simplified test configuration for Task Execution Engine tests.
 * Uses Spring Boot's auto-configuration capabilities.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 12.04.2025
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "cz.rb.task.controller")
public class TaskExecutionEngineTestConfig { }