package com.meetclone.identity.dto;

public record OAuthUserProfile(
        String providerUserId,
        String email,
        String name,
        String avatarUrl
) {}
