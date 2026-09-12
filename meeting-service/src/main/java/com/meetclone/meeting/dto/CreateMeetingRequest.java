package com.meetclone.meeting.dto;

import java.time.Instant;

public record CreateMeetingRequest(
    String title,
    Instant scheduledStart,
    Instant scheduledEnd
) {}
