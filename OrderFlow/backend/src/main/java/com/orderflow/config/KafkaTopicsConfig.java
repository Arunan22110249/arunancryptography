package com.orderflow.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaTopicsConfig {
    @Bean NewTopic orderCreatedTopic() { return topic("order.created"); }
    @Bean NewTopic inventoryReservedTopic() { return topic("inventory.reserved"); }
    @Bean NewTopic inventoryReleasedTopic() { return topic("inventory.released"); }
    @Bean NewTopic inventoryFailedTopic() { return topic("inventory.failed"); }
    @Bean NewTopic paymentAuthorizedTopic() { return topic("payment.authorized"); }
    @Bean NewTopic paymentFailedTopic() { return topic("payment.failed"); }
    @Bean NewTopic orderConfirmedTopic() { return topic("order.confirmed"); }
    @Bean NewTopic orderCancelledTopic() { return topic("order.cancelled"); }
    @Bean NewTopic notificationRequestedTopic() { return topic("notification.requested"); }
    @Bean NewTopic auditEventsTopic() { return topic("audit.events"); }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}
