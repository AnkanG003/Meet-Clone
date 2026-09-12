package com.meetclone.recording.event;

import java.time.Instant;
import java.util.UUID;

public record RecordingReadyEvent(
    UUID recordingId,
    UUID meetingId,
    String storageUrl,
    Long durationSeconds,
    Instant timestamp
) {}
