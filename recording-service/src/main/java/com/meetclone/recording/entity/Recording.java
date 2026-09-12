package com.meetclone.recording.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recordings")
public class Recording {
    @Id
    private UUID id;
    private UUID meetingId;
    private String storageUrl;
    private Long durationSeconds;
    private String status;
    private Instant recordedAt;
}
