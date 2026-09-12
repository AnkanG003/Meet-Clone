package com.meetclone.identity.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "oauth_identities")
public class OAuthIdentity {
    @Id
    private UUID id;
    private UUID userId;
    private String provider;
    private String providerUserId;
}
