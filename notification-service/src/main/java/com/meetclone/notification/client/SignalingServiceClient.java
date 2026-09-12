package com.meetclone.notification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(name = "signaling-service", url = "${services.signaling-service.url:http://localhost:8083}")
public interface SignalingServiceClient {
    @PostMapping("/internal/waiting-room/admit")
    void admitParticipant(@RequestBody Map<String, Object> request);
}
