package com.meetclone.identity.service;

import com.meetclone.identity.dto.LoginResponse;
import com.meetclone.identity.dto.OAuthUserProfile;
import com.meetclone.identity.entity.OAuthIdentity;
import com.meetclone.identity.entity.RefreshToken;
import com.meetclone.identity.entity.User;
import com.meetclone.identity.event.UserEventPublisher;
import com.meetclone.identity.event.UserRegisteredEvent;
import com.meetclone.identity.oauth.OAuthProviderHandler;
import com.meetclone.identity.repository.OAuthIdentityRepository;
import com.meetclone.identity.repository.RefreshTokenRepository;
import com.meetclone.identity.repository.UserRepository;
import com.meetclone.identity.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OAuthIdentityRepository oauthIdentityRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserEventPublisher eventPublisher;

    @Mock
    private OAuthProviderHandler googleHandler;

    private JwtTokenProvider jwtTokenProvider;
    private OAuthService oAuthService;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                "super-secret-key-that-is-at-least-256-bits-long-for-testing-purpose",
                15,
                30
        );

        when(googleHandler.getProviderName()).thenReturn("GOOGLE");

        oAuthService = new OAuthService(
                List.of(googleHandler),
                userRepository,
                oauthIdentityRepository,
                refreshTokenRepository,
                jwtTokenProvider,
                eventPublisher
        );
    }

    @Test
    void getAuthUrl_shouldReturnProviderUrl() {
        when(googleHandler.getAuthorizationUrl()).thenReturn("https://accounts.google.com/auth");

        String url = oAuthService.getAuthUrl("GOOGLE");

        assertEquals("https://accounts.google.com/auth", url);
    }

    @Test
    void getAuthUrl_shouldThrowOnUnsupportedProvider() {
        assertThrows(IllegalArgumentException.class, () -> oAuthService.getAuthUrl("GITHUB"));
    }

    @Test
    void handleCallback_shouldProvisionNewUserAndReturnTokens() {
        OAuthUserProfile profile = new OAuthUserProfile("google-123", "oauth@example.com", "OAuth User", "https://pic.jpg");
        when(googleHandler.fetchUserProfile("auth-code-123")).thenReturn(profile);
        when(oauthIdentityRepository.findByProviderAndProviderUserId("GOOGLE", "google-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("oauth@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponse response = oAuthService.handleCallback("google", "auth-code-123");

        assertNotNull(response);
        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());

        verify(userRepository).save(any(User.class));
        verify(oauthIdentityRepository).save(any(OAuthIdentity.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(eventPublisher).publishUserRegistered(any(UserRegisteredEvent.class));
    }

    @Test
    void handleCallback_shouldAuthenticateExistingIdentity() {
        UUID existingUserId = UUID.randomUUID();
        User existingUser = new User(existingUserId, "oauth@example.com", null, "Existing", null, Instant.now());
        OAuthIdentity existingIdentity = new OAuthIdentity(UUID.randomUUID(), existingUserId, "GOOGLE", "google-123");
        OAuthUserProfile profile = new OAuthUserProfile("google-123", "oauth@example.com", "Existing", "https://new-pic.jpg");

        when(googleHandler.fetchUserProfile("auth-code-123")).thenReturn(profile);
        when(oauthIdentityRepository.findByProviderAndProviderUserId("GOOGLE", "google-123")).thenReturn(Optional.of(existingIdentity));
        when(userRepository.findById(existingUserId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponse response = oAuthService.handleCallback("GOOGLE", "auth-code-123");

        assertNotNull(response);
        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());

        verify(userRepository).save(existingUser);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(eventPublisher, never()).publishUserRegistered(any(UserRegisteredEvent.class));
    }
}
