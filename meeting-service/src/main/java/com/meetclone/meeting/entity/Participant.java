package com.meetclone.meeting.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "participants")
public class Participant {
    @Id
    private UUID id;
    private UUID meetingId;
    private UUID userId;
    private String role;
    private Instant joinedAt;
    private Instant leftAt;
}
