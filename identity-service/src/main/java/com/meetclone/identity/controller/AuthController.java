package com.meetclone.identity.controller;

import com.meetclone.identity.dto.LoginRequest;
import com.meetclone.identity.dto.LoginResponse;
import com.meetclone.identity.dto.RegisterRequest;
import com.meetclone.identity.service.AuthService;
import com.meetclone.identity.service.OAuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final OAuthService oAuthService;
    private final String frontendRedirectUri;

    public AuthController(
            AuthService authService,
            OAuthService oAuthService,
            @Value("${app.oauth2.frontend-redirect-uri:http://localhost:3000/auth/callback}") String frontendRedirectUri
    ) {
        this.authService = authService;
        this.oAuthService = oAuthService;
        this.frontendRedirectUri = frontendRedirectUri;
    }

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

    @GetMapping("/oauth/{provider}/url")
    public Map<String, String> getOAuthUrl(@PathVariable String provider) {
        return Map.of("url", oAuthService.getAuthUrl(provider));
    }

    @GetMapping("/oauth/{provider}/callback")
    public void oauthCallback(
            @PathVariable String provider,
            @RequestParam("code") String code,
            HttpServletResponse response
    ) throws IOException {
        LoginResponse loginResponse = oAuthService.handleCallback(provider, code);
        String redirectUrl = frontendRedirectUri +
                "?accessToken=" + URLEncoder.encode(loginResponse.accessToken(), StandardCharsets.UTF_8) +
                "&refreshToken=" + URLEncoder.encode(loginResponse.refreshToken(), StandardCharsets.UTF_8);
        response.sendRedirect(redirectUrl);
    }

    @PostMapping("/oauth/{provider}/token")
    public LoginResponse oauthToken(
            @PathVariable String provider,
            @RequestParam("code") String code
    ) {
        return oAuthService.handleCallback(provider, code);
    }
}
