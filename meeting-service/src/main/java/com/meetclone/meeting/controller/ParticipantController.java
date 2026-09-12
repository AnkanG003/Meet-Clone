package com.meetclone.meeting.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/meetings/{meetingId}/participants")
public class ParticipantController {
    // PATCH /meetings/{id}/participants/{id} - mute/remove/change role
}
