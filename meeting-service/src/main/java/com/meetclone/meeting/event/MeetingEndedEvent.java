package com.meetclone.meeting.event;

import java.time.Instant;
import java.util.UUID;

public record MeetingEndedEvent(
    UUID meetingId,
    Instant timestamp
) {}
