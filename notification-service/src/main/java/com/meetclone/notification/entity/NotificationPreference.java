package com.meetclone.notification.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {
    @Id
    private UUID userId;
    private boolean emailEnabled;
    private boolean pushEnabled;
    private boolean notifyOnMention;
}
