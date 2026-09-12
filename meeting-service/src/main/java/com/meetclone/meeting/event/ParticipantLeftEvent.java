package com.meetclone.meeting.event;

import java.time.Instant;
import java.util.UUID;

public record ParticipantLeftEvent(
    UUID meetingId,
    UUID userId,
    Instant timestamp
) {}
