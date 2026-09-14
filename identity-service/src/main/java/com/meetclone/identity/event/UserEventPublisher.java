package com.meetclone.identity.event;

public interface UserEventPublisher {
    void publishUserRegistered(UserRegisteredEvent event);
    void publishUserUpdated(UserUpdatedEvent event);
}
