package com.meetclone.identity.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UserEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserRegistered(UserRegisteredEvent event) {
        /* TODO: kafkaTemplate.send("user-registered", ...) */
    }

    public void publishUserUpdated(UserUpdatedEvent event) {
        /* TODO: kafkaTemplate.send("user-updated", ...) */
    }
}
