package cz.rb.task.messaging;

import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

/**
 * Interface to abstract Kafka publishing operations.
 * Makes testing easier by avoiding direct KafkaTemplate mocking.
 *
 * @param <K> Key type
 * @param <V> Value type
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 22.02.2025
 */
public interface KafkaPublisher<K, V> {
    CompletableFuture<SendResult<K, V>> send(String topic, K key, V value);
}

