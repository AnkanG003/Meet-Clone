package com.meetclone.identity.dto;

import java.util.UUID;

public record UserProfileDto(
    UUID id,
    String email,
    String name,
    String avatarUrl
) {}
