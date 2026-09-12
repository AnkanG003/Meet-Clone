package com.meetclone.notification.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RecordingEventConsumer {
    @KafkaListener(topics = "recording-events", groupId = "notification-recording-group")
    public void consumeRecordingEvent(String event) {
        // RecordingReady -> triggers email/push
    }
}
