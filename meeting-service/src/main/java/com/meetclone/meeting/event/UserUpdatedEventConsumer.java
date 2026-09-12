package com.meetclone.meeting.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserUpdatedEventConsumer {
    @KafkaListener(topics = "user-updated-events", groupId = "meeting-service-group")
    public void consumeUserUpdated(String eventPayload) {
        // refreshes cached display name/avatar
    }
}
