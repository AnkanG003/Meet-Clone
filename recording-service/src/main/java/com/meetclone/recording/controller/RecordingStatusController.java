package com.meetclone.recording.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recordings")
public class RecordingStatusController {
    // GET /recordings/{id}/status - polled by frontend
}
