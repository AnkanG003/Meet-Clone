package com.meetclone.identity.dto;

public record RegisterRequest(
    String email,
    String password,
    String name
) {}
