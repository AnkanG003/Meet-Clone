package com.meetclone.identity.event;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
    UUID userId,
    String email,
    String name,
    Instant timestamp
) {}
