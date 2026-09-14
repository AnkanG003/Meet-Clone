package com.meetclone.identity.service;

import com.meetclone.identity.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuthService {
    public LoginResponse handleGoogleCallback(String authCode) {
        throw new UnsupportedOperationException("TODO");
    }
}
