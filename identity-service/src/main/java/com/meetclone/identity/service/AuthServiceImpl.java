package com.meetclone.identity.service;

import com.meetclone.identity.dto.LoginRequest;
import com.meetclone.identity.dto.LoginResponse;
import com.meetclone.identity.dto.RegisterRequest;
import com.meetclone.identity.event.UserEventPublisher;
import com.meetclone.identity.repository.RefreshTokenRepository;
import com.meetclone.identity.repository.UserRepository;
import com.meetclone.identity.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserEventPublisher eventPublisher;

    @Override
    public LoginResponse register(RegisterRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public LoginResponse refresh(String refreshToken) {
        throw new UnsupportedOperationException("TODO");
    }
}
