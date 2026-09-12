package com.meetclone.signaling.recording;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/recording")
public class RecordingBridgeController {
    // internal REST endpoint recording-service calls to tap into pipeline
}
