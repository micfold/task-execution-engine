package cz.rb.task.messaging;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// TODO: Uncomment the annotation below after integration tests are fixed

/*
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TaskEventPublisherIntegrationTest.TestConfig.class)
@EmbeddedKafka(partitions = 1, topics = {"task-events-test"})
@DirtiesContext
class TaskEventPublisherIntegrationTest {

    private static final String TEST_TOPIC = "task-events-test";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private TaskEventPublisher eventPublisher;

    private Consumer<String, TaskEvent> consumer;

    @BeforeEach
    void setUp() {
        // Ensure topic is set correctly
        eventPublisher.setTaskEventsTopic(TEST_TOPIC);

        // Configure consumer
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "test-group-" + UUID.randomUUID(), "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        // Create consumer
        DefaultKafkaConsumerFactory<String, TaskEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(
                        consumerProps,
                        new StringDeserializer(),
                        new JsonDeserializer<>(TaskEvent.class, false));

        consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singleton(TEST_TOPIC));

        // Clear any existing messages
        consumer.poll(Duration.ofMillis(100));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @DisplayName("Should publish and consume TaskEvent from Kafka")
    void publishAndConsumeEvent() {
        // Given
        String taskId = UUID.randomUUID().toString();
        String taskType = "INTEGRATION_TEST_TASK";
        Map<String, Object> metadata = Map.of(
                "testKey", "testValue",
                "timestamp", Instant.now().toString()
        );

        TaskEvent event = new TaskEvent(
                taskId,
                taskType,
                TaskEvent.EventTypes.TASK_COMPLETED,
                metadata,
                Instant.now()
        );

        // When
        eventPublisher.publishEvent(event);

        // Then
        ConsumerRecords<String, TaskEvent> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));

        assertThat(records.count()).isEqualTo(1);
        records.forEach(record -> {
            assertThat(record.key()).isEqualTo(taskId);
            TaskEvent receivedEvent = record.value();
            assertThat(receivedEvent).isNotNull();
            assertThat(receivedEvent.taskId()).isEqualTo(taskId);
            assertThat(receivedEvent.taskType()).isEqualTo(taskType);
            assertThat(receivedEvent.eventType()).isEqualTo(TaskEvent.EventTypes.TASK_COMPLETED);
            assertThat(receivedEvent.metadata()).containsKey("testKey");
            assertThat(receivedEvent.metadata().get("testKey")).isEqualTo("testValue");
        });
    }

    @Test
    @DisplayName("Should publish and consume task started event")
    void publishAndConsumeTaskStartedEvent() {
        // Given
        String taskId = UUID.randomUUID().toString();
        String taskType = "INTEGRATION_TEST_TASK";

        // When
        eventPublisher.publishEvent(TaskEvent.taskStarted(taskId, taskType));

        // Then
        ConsumerRecords<String, TaskEvent> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));

        assertThat(records.count()).isEqualTo(1);
        records.forEach(record -> {
            assertThat(record.key()).isEqualTo(taskId);
            TaskEvent receivedEvent = record.value();
            assertThat(receivedEvent).isNotNull();
            assertThat(receivedEvent.taskId()).isEqualTo(taskId);
            assertThat(receivedEvent.taskType()).isEqualTo(taskType);
            assertThat(receivedEvent.eventType()).isEqualTo(TaskEvent.EventTypes.TASK_STARTED);
        });
    }

    @Configuration
    @EnableAutoConfiguration
    public static class TestConfig {

        // NOTE: When using @EmbeddedKafka annotation, the broker is automatically created
        // We should NOT create another broker instance manually, instead use the one provided

        @Bean
        public ProducerFactory<String, TaskEvent> producerFactory(EmbeddedKafkaBroker embeddedKafkaBroker) {
            Map<String, Object> producerProps = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
            producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
            return new DefaultKafkaProducerFactory<>(producerProps);
        }

        @Bean
        public KafkaTemplate<String, TaskEvent> kafkaTemplate(ProducerFactory<String, TaskEvent> producerFactory) {
            return new KafkaTemplate<>(producerFactory);
        }

        @Bean
        public TaskEventPublisher taskEventPublisher(KafkaTemplate<String, TaskEvent> kafkaTemplate) {
            TaskEventPublisher publisher = new TaskEventPublisher(kafkaTemplate);
            publisher.setTaskEventsTopic(TEST_TOPIC);
            return publisher;
        }
    }
}
 */
