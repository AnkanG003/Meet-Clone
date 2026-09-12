package com.meetclone.identity.dto;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    long expiresIn
) {}
