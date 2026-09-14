package com.meetclone.identity.event;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserRegistered(UserRegisteredEvent event) {
        /* TODO: kafkaTemplate.send("user-registered", ...) */
    }

    public void publishUserUpdated(UserUpdatedEvent event) {
        /* TODO: kafkaTemplate.send("user-updated", ...) */
    }
}
