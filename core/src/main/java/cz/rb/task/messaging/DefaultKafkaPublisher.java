package cz.rb.task.messaging;

import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Default implementation of KafkaPublisher using KafkaTemplate.
 */
@Component
class DefaultKafkaPublisher<K, V> implements KafkaPublisher<K, V> {
    private final org.springframework.kafka.core.KafkaTemplate<K, V> kafkaTemplate;

    DefaultKafkaPublisher(org.springframework.kafka.core.KafkaTemplate<K, V> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public CompletableFuture<SendResult<K, V>> send(String topic, K key, V value) {
        return kafkaTemplate.send(topic, key, value);
    }
}
