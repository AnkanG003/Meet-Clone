package com.meetclone.meeting.event;

import java.time.Instant;
import java.util.UUID;

public record MeetingStartedEvent(
    UUID meetingId,
    String code,
    UUID hostId,
    Instant timestamp
) {}
