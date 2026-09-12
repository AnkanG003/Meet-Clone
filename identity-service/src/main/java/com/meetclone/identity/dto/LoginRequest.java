package com.meetclone.identity.dto;

public record LoginRequest(
    String email,
    String password
) {}
