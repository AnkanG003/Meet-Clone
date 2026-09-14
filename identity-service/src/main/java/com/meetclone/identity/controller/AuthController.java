package com.meetclone.identity.controller;

import com.meetclone.identity.dto.LoginRequest;
import com.meetclone.identity.dto.LoginResponse;
import com.meetclone.identity.dto.RegisterRequest;
import com.meetclone.identity.service.AuthService;
import com.meetclone.identity.service.OAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final OAuthService oAuthService;

    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@RequestParam String refreshToken) {
        return authService.refresh(refreshToken);
    }

    @GetMapping("/oauth/google/callback")
    public LoginResponse googleCallback(@RequestParam String code) {
        return oAuthService.handleGoogleCallback(code);
    }
}
