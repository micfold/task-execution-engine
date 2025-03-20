package cz.rb.task.config;

import cz.rb.task.schema.TEESchemaCreationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Auto-configuration for Task Execution Engine.
 * Provides beans for schema creation and management.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 20.03.2025
 */
@Configuration
@ComponentScan(basePackages = {
        "cz.rb.task.engine",
        "cz.rb.task.persistence",
        "cz.rb.task.schema"
})
public class TEEAutoConfiguration {

    /**
     * Creates the schema creation service if a DataSource is available.
     *
     * @param dataSource The application's data source
     * @return The schema creation service
     */
    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean
    public TEESchemaCreationService schemaCreationService(final DataSource dataSource) {
        return new TEESchemaCreationService(dataSource);
    }

    /**
     * Auto-initializes the schema if enabled via properties.
     * Applications can disable this and manage schema creation manually.
     */
    @Configuration
    @ConditionalOnProperty(name = "task.persistence.auto-initialize", havingValue = "true", matchIfMissing = false)
    static class SchemaAutoInitialization {

        /**
         * Initializes the schema automatically on startup if enabled.
         *
         * @param schemaService The schema creation service
         * @return An initializer that triggers schema creation
         */
        @Bean
        public SchemaInitializer schemaInitializer(final TEESchemaCreationService schemaService) {
            return new SchemaInitializer(schemaService);
        }

        /**
         * Helper class that triggers schema initialization during application startup.
         */
        static class SchemaInitializer {
            public SchemaInitializer(final TEESchemaCreationService schemaService) {
                // Create schema with default options during initialization
                schemaService.createSchema(cz.rb.task.schema.SchemaOptions.builder().build());
            }
        }
    }
}
