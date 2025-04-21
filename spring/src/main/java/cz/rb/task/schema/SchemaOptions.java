package cz.rb.task.schema;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Configuration options for TEE schema creation.
 * Allows customization of table names, prefixes, and features.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 20.03.2025
 */
@Getter
@Builder
@ToString
public class SchemaOptions {

    /**
     * Schema name where tables should be created.
     * Default is "public".
     */
    @Builder.Default
    // TODO: Create a property
    private String schemaName = "public";

    /**
     * Prefix for all table names.
     * Useful for integrating with existing applications.
     * Default is empty string (no prefix).
     */
    @Builder.Default
    // TODO: Create a property
    private String tablePrefix = "";

    /**
     * Name of the tasks table (without prefix).
     * Default is "tasks".
     */
    @Builder.Default
    // TODO: Create a property
    private String tasksTableName = "tasks";

    /**
     * Whether to create constraints and check constraints.
     * Default is true.
     */
    @Builder.Default
    // TODO: Create a property
    private boolean enableConstraints = true;

    /**
     * Whether to drop existing tables before creation.
     * Use with caution! Default is false.
     */
    @Builder.Default
    // TODO: Create a property
    private boolean dropExistingTables = false;
}
