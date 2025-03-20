-- data.sql
-- Sample data for testing and development
-- Author: Michael Foldyna
-- Created: 22/02/2025

-- Insert sample tasks
INSERT INTO tasks (
    task_id,
    type,
    status,
    data,
    handler_url,
    retry_count,
    created_at,
    updated_at
) VALUES
      (
          'test-task-1',
          'DOCUMENT_PROCESSING',
          'COMPLETED',
          '{"documentId": "doc-1", "priority": "HIGH"}',
          'http://localhost:8080/api/documents',
          0,
          NOW() - INTERVAL '1 hour',
          NOW() - INTERVAL '30 minutes'
      ),
      (
          'test-task-2',
          'EMAIL_NOTIFICATION',
          'PENDING',
          '{"recipientId": "user-1", "template": "WELCOME"}',
          'http://localhost:8080/api/notifications',
          0,
          NOW() - INTERVAL '30 minutes',
          NOW() - INTERVAL '30 minutes'
      ),
      (
          'test-task-3',
          'DATA_SYNC',
          'FAILED',
          '{"sourceSystem": "CRM", "entityId": "customer-1"}',
          'http://localhost:8080/api/sync',
          2,
          NOW() - INTERVAL '2 hours',
          NOW() - INTERVAL '1 hour'
      );

-- Insert sample task events
INSERT INTO task_events (
    event_id,
    task_id,
    event_type,
    metadata,
    created_at
) VALUES
      (
          'event-1',
          'test-task-1',
          'TASK_CREATED',
          '{"initiator": "system"}',
          NOW() - INTERVAL '1 hour'
      ),
      (
          'event-2',
          'test-task-1',
          'TASK_COMPLETED',
          '{"processingTime": "PT30M"}',
          NOW() - INTERVAL '30 minutes'
      ),
      (
          'event-3',
          'test-task-2',
          'TASK_CREATED',
          '{"initiator": "user-1"}',
          NOW() - INTERVAL '30 minutes'
      ),
      (
          'event-4',
          'test-task-3',
          'TASK_CREATED',
          '{"initiator": "scheduler"}',
          NOW() - INTERVAL '2 hours'
      ),
      (
          'event-5',
          'test-task-3',
          'RETRY_ATTEMPTED',
          '{"attempt": 1, "error": "Connection timeout"}',
          NOW() - INTERVAL '90 minutes'
      ),
      (
          'event-6',
          'test-task-3',
          'TASK_FAILED',
          '{"finalError": "Max retries exceeded"}',
          NOW() - INTERVAL '1 hour'
      );