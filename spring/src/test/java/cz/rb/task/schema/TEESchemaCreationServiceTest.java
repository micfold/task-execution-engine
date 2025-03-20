package cz.rb.task.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TEESchemaCreationService.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 20.03.2025
 */
@ExtendWith(MockitoExtension.class)
class TEESchemaCreationServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(TEESchemaCreationServiceTest.class);

    private TEESchemaCreationService schemaService;

    @Mock
    private DataSource dataSource;

    // SQL script content for testing
    private static final String TEST_SQL_SCRIPT =
            """
                    -- Test SQL Script
                    CREATE SCHEMA IF NOT EXISTS ${schema_name};
                    
                    CREATE TABLE IF NOT EXISTS ${schema_name}.${tasks_table} (
                        task_id VARCHAR(36) PRIMARY KEY,
                        type VARCHAR(100) NOT NULL
                    );
                    
                    CREATE INDEX IF NOT EXISTS ${table_prefix}idx_tasks_status ON ${schema_name}.${tasks_table}(status);""";

    @BeforeEach
    void setUp() {
        // Create service with mocked dependencies
        schemaService = new SchemaServiceWithMockedResourceLoading(dataSource);
    }

    /**
     * A subclass of TEESchemaCreationService that overrides the resource loading for testing
     */
    private static class SchemaServiceWithMockedResourceLoading extends TEESchemaCreationService {
        private static final Logger logger = LoggerFactory.getLogger(SchemaServiceWithMockedResourceLoading.class);

        public SchemaServiceWithMockedResourceLoading(DataSource dataSource) {
            super(dataSource);
        }

        @Override
        protected String readResourceAndReplacePlaceholders(final Resource resource, SchemaOptions options) {
            String content = TEST_SQL_SCRIPT;
            logger.info("Original SQL script template:\n{}", content);

            // Replace placeholders
            content = content.replace("${schema_name}", options.getSchemaName());
            content = content.replace("${tasks_table}", options.getTablePrefix() + options.getTasksTableName());
            content = content.replace("${table_prefix}", options.getTablePrefix());

            logger.info("Processed SQL script with options {}:\n{}", options, content);
            return content;
        }
    }

    @Test
    @DisplayName("Should create schema with default options")
    void createSchemaWithDefaultOptions() throws SQLException {
        // Given
        final SchemaOptions options = SchemaOptions.builder().build();
        final Connection connection = mock(Connection.class);
        final Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);

        // Log test start
        logger.info("Starting test: createSchemaWithDefaultOptions");

        // When
        schemaService.createSchema(options);

        // Then
        verify(dataSource).getConnection();
        verify(connection).createStatement();

        // Verify SQL statement execution
        final ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(statement, times(3)).execute(sqlCaptor.capture());

        final List<String> executedStatements = sqlCaptor.getAllValues();
        assertThat(executedStatements).hasSize(3);

        assertThat(executedStatements.get(0))
                .contains("CREATE SCHEMA IF NOT EXISTS public")
                .doesNotContain("${schema_name}");

        assertThat(executedStatements.get(1))
                .contains("CREATE TABLE IF NOT EXISTS public.tasks")
                .doesNotContain("${tasks_table}");
    }

    @Test
    @DisplayName("Should create schema with custom options")
    void createSchemaWithCustomOptions() throws SQLException {
        // Given
        final SchemaOptions options = SchemaOptions.builder()
                .schemaName("custom_schema")
                .tablePrefix("app_")
                .tasksTableName("my_tasks")
                .build();

        final Connection connection = mock(Connection.class);
        final Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);

        // Log test start
        logger.info("Starting test: createSchemaWithCustomOptions");
        logger.info("Using schema options: {}", options);

        // When
        schemaService.createSchema(options);

        // Then
        verify(dataSource).getConnection();
        verify(connection).createStatement();

        // Verify SQL statement execution
        final ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(statement, times(3)).execute(sqlCaptor.capture());

        final List<String> executedStatements = sqlCaptor.getAllValues();
        assertThat(executedStatements).hasSize(3);

        assertThat(executedStatements.get(0))
                .contains("CREATE SCHEMA IF NOT EXISTS custom_schema")
                .doesNotContain("${schema_name}");

        assertThat(executedStatements.get(1))
                .contains("CREATE TABLE IF NOT EXISTS custom_schema.app_my_tasks")
                .doesNotContain("${tasks_table}");

        assertThat(executedStatements.get(2))
                .contains("CREATE INDEX IF NOT EXISTS app_idx_tasks_status ON custom_schema.app_my_tasks")
                .doesNotContain("${table_prefix}");
    }

    @Test
    @DisplayName("Should handle SQL execution errors gracefully")
    void shouldHandleSqlErrors() throws SQLException {
        // Given
        final SchemaOptions options = SchemaOptions.builder().build();
        final Connection connection = mock(Connection.class);
        final Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenThrow(new SQLException("SQL execution error"));

        logger.info("Starting test: shouldHandleSqlErrors");

        // When
        schemaService.createSchema(options);

        // Then
        // Should not throw exception but log the error
        verify(dataSource).getConnection();
        verify(connection).createStatement();
        verify(statement).execute(anyString());

        logger.info("Successfully verified error handling without exception");
    }

    @Test
    @DisplayName("Should handle connection errors gracefully")
    void shouldHandleConnectionErrors() throws SQLException {
        // Given
        final SchemaOptions options = SchemaOptions.builder().build();

        // Mock a connection error
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection error"));

        logger.info("Starting test: shouldHandleConnectionErrors");

        // When
        schemaService.createSchema(options);

        // Then
        // Should not throw exception but log the error
        verify(dataSource).getConnection();

        logger.info("Successfully verified connection error handling without exception");
    }
}