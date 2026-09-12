package com.meetclone.signaling.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/tokens")
public class InternalTokenController {
    // POST /internal/tokens - called by meeting-service
}
