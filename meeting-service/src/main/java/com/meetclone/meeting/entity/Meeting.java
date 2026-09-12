package com.meetclone.meeting.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meetings")
public class Meeting {
    @Id
    private UUID id;
    private String code;
    private UUID hostId;
    private String title;
    private Instant scheduledStart;
    private Instant scheduledEnd;
    private String status;
}
