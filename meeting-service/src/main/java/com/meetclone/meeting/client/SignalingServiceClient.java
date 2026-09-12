package com.meetclone.meeting.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(name = "signaling-service", url = "${services.signaling-service.url:http://localhost:8083}")
public interface SignalingServiceClient {
    @PostMapping("/internal/tokens")
    Map<String, Object> createSignalingToken(@RequestBody Map<String, Object> request);
}
