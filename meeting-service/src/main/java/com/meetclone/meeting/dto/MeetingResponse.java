package com.meetclone.meeting.dto;

import java.time.Instant;
import java.util.UUID;

public record MeetingResponse(
    UUID id,
    String code,
    UUID hostId,
    String title,
    String status,
    Instant scheduledStart,
    Instant scheduledEnd
) {}
