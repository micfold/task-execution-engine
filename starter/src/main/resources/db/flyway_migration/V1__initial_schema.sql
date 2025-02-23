-- V1__initial_schema.sql
-- Initial database schema for task execution engine
-- Author: Michael Foldyna
-- Created: 22/02/2025

-- Tasks table for storing task execution information
CREATE TABLE tasks (
                       task_id VARCHAR(36) PRIMARY KEY,
                       type VARCHAR(100) NOT NULL,
                       status VARCHAR(20) NOT NULL,
                       data JSONB,
                       handler_url VARCHAR(255),
                       retry_count INT DEFAULT 0,
                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP NOT NULL,
                       CONSTRAINT chk_tasks_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'DEAD_LETTER'))
);

-- Indexes for performance optimization
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_type_status ON tasks(type, status);
CREATE INDEX idx_tasks_updated_at ON tasks(updated_at);

-- Add comments for documentation
COMMENT ON TABLE tasks IS 'Stores task execution information and status';
COMMENT ON COLUMN tasks.task_id IS 'Unique identifier for the task';
COMMENT ON COLUMN tasks.type IS 'Type of task to be executed';
COMMENT ON COLUMN tasks.status IS 'Current status of the task';
COMMENT ON COLUMN tasks.data IS 'Task-specific data stored as JSON';
COMMENT ON COLUMN tasks.handler_url IS 'URL of the service handling this task type';
COMMENT ON COLUMN tasks.retry_count IS 'Number of retry attempts for this task';
COMMENT ON COLUMN tasks.created_at IS 'Timestamp when the task was created';
COMMENT ON COLUMN tasks.updated_at IS 'Timestamp of the last status update';