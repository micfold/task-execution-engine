package cz.rb.task.persistence;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Test application configuration for repository tests.
 * Provides a Spring context for running integration tests.
 */
@SpringBootApplication
public class TestRepositoryApplication {
    // This class exists only to provide a context for repository tests
}