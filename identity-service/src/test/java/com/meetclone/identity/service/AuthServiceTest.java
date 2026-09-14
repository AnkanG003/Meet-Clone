package com.meetclone.identity.service;

import com.meetclone.identity.dto.LoginRequest;
import com.meetclone.identity.dto.LoginResponse;
import com.meetclone.identity.dto.RegisterRequest;
import com.meetclone.identity.entity.RefreshToken;
import com.meetclone.identity.entity.User;
import com.meetclone.identity.event.UserEventPublisher;
import com.meetclone.identity.event.UserRegisteredEvent;
import com.meetclone.identity.exception.InvalidCredentialsException;
import com.meetclone.identity.repository.RefreshTokenRepository;
import com.meetclone.identity.repository.UserRepository;
import com.meetclone.identity.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserEventPublisher eventPublisher;

    private JwtTokenProvider jwtTokenProvider;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                "super-secret-key-that-is-at-least-256-bits-long-for-testing-purpose",
                15,
                30
        );
        authService = new AuthServiceImpl(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtTokenProvider,
                eventPublisher
        );
    }

    @Test
    void register_shouldHashPasswordAndSaveUser() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "Test User");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponse response = authService.register(request);

        assertNotNull(response);
        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());

        assertTrue(jwtTokenProvider.validate(response.accessToken()));
        assertTrue(jwtTokenProvider.validate(response.refreshToken()));

        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(eventPublisher).publishUserRegistered(any(UserRegisteredEvent.class));
    }

    @Test
    void register_shouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "Test User");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(new User()));

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_shouldIssueTokensOnValidCredentials() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        User user = new User(UUID.randomUUID(), "test@example.com", "hashedPassword", "Test User", null, Instant.now());

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_shouldThrowOnInvalidPassword() {
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
        User user = new User(UUID.randomUUID(), "test@example.com", "hashedPassword", "Test User", null, Instant.now());

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void refresh_shouldRotateToken() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "test@example.com", "hashedPassword", "Test User", null, Instant.now());
        String oldToken = jwtTokenProvider.generateRefreshToken(user);
        RefreshToken tokenEntity = new RefreshToken(UUID.randomUUID(), oldToken, userId, Instant.now().plus(1, ChronoUnit.DAYS), false);

        when(refreshTokenRepository.findByToken(oldToken)).thenReturn(Optional.of(tokenEntity));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        LoginResponse response = authService.refresh(oldToken);

        assertNotNull(response);
        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertTrue(tokenEntity.isRevoked());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }
}
