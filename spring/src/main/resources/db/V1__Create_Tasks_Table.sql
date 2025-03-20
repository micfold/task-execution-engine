-- V1__Create_Tasks_Table.sql
-- Task Execution Engine core table schema with placeholders for customization
-- Author: Michael Foldyna
-- Created: 22/02/2025

-- Create schema if it doesn't exist (PostgreSQL specific)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = '${schema_name}') THEN
        EXECUTE 'CREATE SCHEMA ${schema_name}';
END IF;
END
$$;

-- Tasks table for storing task execution information
CREATE TABLE IF NOT EXISTS ${schema_name}.${tasks_table} (
                                                             task_id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    data JSONB,
    handler_url VARCHAR(255),
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
    );

-- Add constraints if enabled
DO $$
BEGIN
    -- Check if constraints are enabled and not already exist
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = '${tasks_table}_chk_status'
    ) THEN
        EXECUTE 'ALTER TABLE ${schema_name}.${tasks_table}
                ADD CONSTRAINT ${tasks_table}_chk_status
                CHECK (status IN (''PENDING'', ''IN_PROGRESS'', ''COMPLETED'', ''FAILED'', ''DEAD_LETTER''))';
END IF;
END
$$;

-- Indexes for performance optimization
CREATE INDEX IF NOT EXISTS ${tasks_table}_idx_status
    ON ${schema_name}.${tasks_table}(status);

CREATE INDEX IF NOT EXISTS ${tasks_table}_idx_type_status
    ON ${schema_name}.${tasks_table}(type, status);

CREATE INDEX IF NOT EXISTS ${tasks_table}_idx_updated_at
    ON ${schema_name}.${tasks_table}(updated_at);

CREATE INDEX IF NOT EXISTS ${tasks_table}_idx_status_updated_at
    ON ${schema_name}.${tasks_table}(status, updated_at);

-- Add comments for documentation
COMMENT ON TABLE ${schema_name}.${tasks_table} IS 'Stores task execution information and status';
COMMENT ON COLUMN ${schema_name}.${tasks_table}.task_id IS 'Unique identifier for the task';
COMMENT ON COLUMN ${schema_name}.${tasks_table}.type IS 'Type of task to be executed';
COMMENT ON COLUMN ${schema_name}.${tasks_table}.status IS 'Current status of the task';
COMMENT ON COLUMN ${schema_name}.${tasks_table}.data IS 'Task-specific data stored as JSON';
COMMENT ON COLUMN ${schema_name}.${tasks_table}.handler_url IS 'URL of the service handling this task type';
COMMENT ON COLUMN ${schema_name}.${tasks_table}.retry_count IS 'Number of retry attempts for this task';
COMMENT ON COLUMN ${schema_name}.${tasks_table}.created_at IS 'Timestamp when the task was created';
COMMENT ON COLUMN ${schema_name}.${tasks_table}.updated_at IS 'Timestamp of the last status update';