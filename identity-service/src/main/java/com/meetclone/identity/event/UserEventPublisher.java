package com.meetclone.identity.event;

import org.springframework.stereotype.Component;

@Component
public class UserEventPublisher {
    // wraps KafkaTemplate.send(...) for user events
}
