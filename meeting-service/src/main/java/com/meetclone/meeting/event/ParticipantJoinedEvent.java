package com.meetclone.meeting.event;

import java.time.Instant;
import java.util.UUID;

public record ParticipantJoinedEvent(
    UUID meetingId,
    UUID userId,
    String role,
    Instant timestamp
) {}
