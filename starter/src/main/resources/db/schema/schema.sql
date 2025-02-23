-- schema.sql
-- Complete schema for task execution engine (for reference)
-- Author: Michael Foldyna
-- Created: 22/02/2025

-- Drop existing tables if they exist
DROP TABLE IF EXISTS task_events;
DROP TABLE IF EXISTS tasks;

-- Tasks table
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

-- Task Events table
CREATE TABLE task_events (
                             event_id VARCHAR(36) PRIMARY KEY,
                             task_id VARCHAR(36) NOT NULL,
                             event_type VARCHAR(50) NOT NULL,
                             metadata JSONB,
                             created_at TIMESTAMP NOT NULL,
                             CONSTRAINT fk_task_events_task FOREIGN KEY (task_id)
                                 REFERENCES tasks(task_id) ON DELETE CASCADE,
                             CONSTRAINT chk_event_type CHECK (event_type IN (
                                                                             'TASK_CREATED',
                                                                             'TASK_STARTED',
                                                                             'TASK_COMPLETED',
                                                                             'TASK_FAILED',
                                                                             'RETRY_ATTEMPTED',
                                                                             'MOVED_TO_DLQ',
                                                                             'RECOVERED_FROM_DLQ'
                                 ))
);

-- Indexes
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_type_status ON tasks(type, status);
CREATE INDEX idx_tasks_updated_at ON tasks(updated_at);
CREATE INDEX idx_task_events_task_id ON task_events(task_id);
CREATE INDEX idx_task_events_created_at ON task_events(created_at);
CREATE INDEX idx_task_events_type ON task_events(event_type);

-- Comments
COMMENT ON TABLE tasks IS 'Stores task execution information and status';
COMMENT ON TABLE task_events IS 'Audit log of task lifecycle events';