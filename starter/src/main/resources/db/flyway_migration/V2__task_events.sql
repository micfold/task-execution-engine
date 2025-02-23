-- V2__task_events.sql
-- Add task events table for audit logging
-- Author: Michael Foldyna
-- Created: 22/02/2025

-- Task Events table for audit logging
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

-- Indexes for efficient querying
CREATE INDEX idx_task_events_task_id ON task_events(task_id);
CREATE INDEX idx_task_events_created_at ON task_events(created_at);
CREATE INDEX idx_task_events_type ON task_events(event_type);

-- Add comments for documentation
COMMENT ON TABLE task_events IS 'Audit log of task lifecycle events';
COMMENT ON COLUMN task_events.event_id IS 'Unique identifier for the event';
COMMENT ON COLUMN task_events.task_id IS 'Reference to the associated task';
COMMENT ON COLUMN task_events.event_type IS 'Type of event that occurred';
COMMENT ON COLUMN task_events.metadata IS 'Event-specific metadata stored as JSON';
COMMENT ON COLUMN task_events.created_at IS 'Timestamp when the event occurred';