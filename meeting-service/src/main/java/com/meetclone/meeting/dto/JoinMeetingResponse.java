package com.meetclone.meeting.dto;

public record JoinMeetingResponse(
    String signalingToken,
    String role,
    String signalingWebSocketUrl
) {}
