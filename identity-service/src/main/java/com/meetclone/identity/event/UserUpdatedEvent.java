package com.meetclone.identity.event;

import java.time.Instant;
import java.util.UUID;

public record UserUpdatedEvent(
    UUID userId,
    String name,
    String avatarUrl,
    Instant occurredAt
) {}
