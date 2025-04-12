-- Schema for H2 in-memory database for testing
-- This is a simplified version of the schema.sql with H2-compatible syntax

DROP TABLE IF EXISTS task_events;
DROP TABLE IF EXISTS tasks;

-- Tasks table
CREATE TABLE tasks (
                       task_id VARCHAR(36) PRIMARY KEY,
                       type VARCHAR(100) NOT NULL,
                       status VARCHAR(20) NOT NULL,
                       data JSON,
                       retry_count INT DEFAULT 0,
                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP NOT NULL
);

-- Task Events table
CREATE TABLE task_events (
                             event_id VARCHAR(36) PRIMARY KEY,
                             task_id VARCHAR(36) NOT NULL,
                             event_type VARCHAR(50) NOT NULL,
                             metadata JSON,
                             created_at TIMESTAMP NOT NULL,
                             FOREIGN KEY (task_id) REFERENCES tasks(task_id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_type_status ON tasks(type, status);
CREATE INDEX idx_tasks_updated_at ON tasks(updated_at);
CREATE INDEX idx_task_events_task_id ON task_events(task_id);
CREATE INDEX idx_task_events_created_at ON task_events(created_at);
CREATE INDEX idx_task_events_type ON task_events(event_type);