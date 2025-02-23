package cz.rb.task.messaging;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes task lifecycle events to Kafka.
 * Handles event creation and delivery for task state changes.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 22.02.2025
 */
@Slf4j
@Component
public class TaskEventPublisher {

    private final KafkaTemplate<String, TaskEvent> kafkaTemplate;

    @Value("${task.kafka.topic.events}")
    private String taskEventsTopic;

    public TaskEventPublisher(KafkaTemplate<String, TaskEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Generic method to publish any task event.
     *
     * @param event The event to publish
     */
    public void publishEvent(final TaskEvent event) {
        kafkaTemplate.send(taskEventsTopic, event.taskId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Published task event: {}, type: {}",
                                event.taskId(), event.eventType());
                    } else {
                        log.error("Failed to publish task event: {}, type: {}, error: {}",
                                event.taskId(), event.eventType(), ex.getMessage(), ex);
                    }
                });
    }
}