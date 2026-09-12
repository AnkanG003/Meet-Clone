package com.meetclone.meeting.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    @Id
    private UUID id;
    private UUID meetingId;
    private UUID senderId;
    private String content;
    private Instant sentAt;
}
