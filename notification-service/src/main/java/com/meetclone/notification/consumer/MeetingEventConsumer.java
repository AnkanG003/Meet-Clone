package com.meetclone.notification.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MeetingEventConsumer {
    @KafkaListener(topics = "meeting-events", groupId = "notification-meeting-group")
    public void consumeMeetingEvent(String event) {
        // MeetingStarted/ParticipantJoined/Left/MeetingEnded
    }
}
