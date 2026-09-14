package com.meetclone.identity.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingUserEventPublisher implements UserEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingUserEventPublisher.class);

    @Override
    public void publishUserRegistered(UserRegisteredEvent event) {
        log.info("User registered event: userId={}, email={}", event.userId(), event.email());
    }

    @Override
    public void publishUserUpdated(UserUpdatedEvent event) {
        log.info("User updated event: userId={}, name={}", event.userId(), event.name());
    }
}
