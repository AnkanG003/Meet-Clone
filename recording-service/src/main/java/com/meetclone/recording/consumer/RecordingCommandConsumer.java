package com.meetclone.recording.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RecordingCommandConsumer {
    @KafkaListener(topics = "recording-command-topic", groupId = "recording-service-group")
    public void consumeRecordingCommand(String message) {
        // receives "start recording" command from meeting-service
    }
}
