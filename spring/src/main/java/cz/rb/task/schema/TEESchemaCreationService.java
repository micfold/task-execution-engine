package cz.rb.task.schema;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for creating the Task Execution Engine database schema.
 * This service dynamically creates the necessary database objects based on configuration.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 20.03.2025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TEESchemaCreationService {

    private final DataSource dataSource;

    private static final String BASE_SCHEMA_PATH = "db/migration/";
    private static final String TASKS_TABLE_SCRIPT = "V1__Create_Tasks_Table.sql";

    /**
     * Creates the database schema for Task Execution Engine.
     * Applies transformations based on the provided options.
     *
     * @param options Configuration options for schema creation
     * @return true if schema was created successfully, false otherwise
     */
    public void createSchema(final SchemaOptions options) {
        log.info("Creating TEE schema with options: {}", options);
        try {
            // Create tasks table
            applyScript(options);

            log.info("TEE schema created successfully");
        } catch (Exception e) {
            log.error("Failed to create TEE schema", e);
        }
    }

    /**
     * Applies a SQL script with transformations based on the provided options.
     *
     * @param options Configuration options
     * @throws IOException  If script cannot be read
     * @throws SQLException If script execution fails
     */
    private void applyScript(final SchemaOptions options) throws IOException, SQLException {
        final Resource resource = new ClassPathResource(BASE_SCHEMA_PATH + TEESchemaCreationService.TASKS_TABLE_SCRIPT);
        final String sql = readResourceAndReplacePlaceholders(resource, options);

        try (Connection conn = dataSource.getConnection();
             final Statement stmt = conn.createStatement()) {

            // Split the script into individual statements
            final List<String> statements = splitSqlStatements(sql);

            for (final String statement : statements) {
                if (!statement.trim().isEmpty()) {
                    stmt.execute(statement);
                }
            }
        }
    }

    /**
     * Reads a resource file and replaces placeholders with values from options.
     *
     * @param resource The resource to read
     * @param options  Configuration options with replacement values
     * @return Transformed SQL script
     * @throws IOException If resource cannot be read
     */
    protected String readResourceAndReplacePlaceholders(final Resource resource, SchemaOptions options) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String content = reader.lines().collect(Collectors.joining("\n"));

            // Replace placeholders
            content = content.replace("${schema_name}", options.getSchemaName());
            content = content.replace("${tasks_table}", options.getTablePrefix() + options.getTasksTableName());
            content = content.replace("${table_prefix}", options.getTablePrefix());

            return content;
        }
    }

    /**
     * Splits a SQL script into individual statements.
     *
     * @param script The SQL script
     * @return List of individual SQL statements
     */
    private List<String> splitSqlStatements(final String script) {
        final List<String> statements = new ArrayList<>();

        // Simple statement splitting by semicolon (could be enhanced for more complex SQL)
        StringBuilder currentStatement = new StringBuilder();
        for (String line : script.split("\n")) {
            // Skip comments
            if (line.trim().startsWith("--")) {
                continue;
            }

            currentStatement.append(line).append("\n");

            if (line.trim().endsWith(";")) {
                statements.add(currentStatement.toString());
                currentStatement = new StringBuilder();
            }
        }

        // Add any remaining statement without semicolon
        if (!currentStatement.isEmpty()) {
            statements.add(currentStatement.toString());
        }

        return statements;
    }
}
