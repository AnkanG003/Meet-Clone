package com.meetclone.identity.service;

import com.meetclone.identity.dto.LoginResponse;
import com.meetclone.identity.dto.OAuthUserProfile;
import com.meetclone.identity.entity.OAuthIdentity;
import com.meetclone.identity.entity.RefreshToken;
import com.meetclone.identity.entity.User;
import com.meetclone.identity.event.UserEventPublisher;
import com.meetclone.identity.event.UserRegisteredEvent;
import com.meetclone.identity.exception.UserNotFoundException;
import com.meetclone.identity.oauth.OAuthProviderHandler;
import com.meetclone.identity.repository.OAuthIdentityRepository;
import com.meetclone.identity.repository.RefreshTokenRepository;
import com.meetclone.identity.repository.UserRepository;
import com.meetclone.identity.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OAuthService {

    private static final Logger log = LoggerFactory.getLogger(OAuthService.class);

    private final Map<String, OAuthProviderHandler> providerHandlers;
    private final UserRepository userRepository;
    private final OAuthIdentityRepository oauthIdentityRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserEventPublisher eventPublisher;

    public OAuthService(
            List<OAuthProviderHandler> handlers,
            UserRepository userRepository,
            OAuthIdentityRepository oauthIdentityRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            UserEventPublisher eventPublisher
    ) {
        this.providerHandlers = handlers.stream()
                .collect(Collectors.toMap(h -> h.getProviderName().toUpperCase(Locale.ROOT), h -> h));
        this.userRepository = userRepository;
        this.oauthIdentityRepository = oauthIdentityRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.eventPublisher = eventPublisher;
    }

    public String getAuthUrl(String provider) {
        OAuthProviderHandler handler = getHandler(provider);
        return handler.getAuthorizationUrl();
    }

    public String getGoogleAuthUrl() {
        return getAuthUrl("GOOGLE");
    }

    @Transactional
    public LoginResponse handleCallback(String provider, String authCode) {
        String normalizedProvider = provider.toUpperCase(Locale.ROOT);
        OAuthProviderHandler handler = getHandler(normalizedProvider);
        OAuthUserProfile profile = handler.fetchUserProfile(authCode);

        User user = resolveOrCreateUser(normalizedProvider, profile);

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        RefreshToken refreshTokenEntity = new RefreshToken(
                UUID.randomUUID(),
                refreshToken,
                user.getId(),
                Instant.now().plus(30, ChronoUnit.DAYS),
                false
        );
        refreshTokenRepository.save(refreshTokenEntity);

        log.info("[AUTH] {} OAuth login successful for email: {} (userId: {})", normalizedProvider, user.getEmail(), user.getId());

        return new LoginResponse(accessToken, refreshToken);
    }

    @Transactional
    public LoginResponse handleGoogleCallback(String authCode) {
        return handleCallback("GOOGLE", authCode);
    }

    private OAuthProviderHandler getHandler(String provider) {
        String key = provider.toUpperCase(Locale.ROOT);
        OAuthProviderHandler handler = providerHandlers.get(key);
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported OAuth provider: " + provider);
        }
        return handler;
    }

    private User resolveOrCreateUser(String provider, OAuthUserProfile profile) {
        Optional<OAuthIdentity> existingIdentity =
                oauthIdentityRepository.findByProviderAndProviderUserId(provider, profile.providerUserId());

        if (existingIdentity.isPresent()) {
            User user = userRepository.findById(existingIdentity.get().getUserId())
                    .orElseThrow(() -> new UserNotFoundException("User not found for OAuth identity"));

            if (profile.avatarUrl() != null && !profile.avatarUrl().equals(user.getAvatarUrl())) {
                user.setAvatarUrl(profile.avatarUrl());
                user = userRepository.save(user);
            }
            log.info("[AUTH] Existing {} OAuth user authenticated: {}", provider, user.getEmail());
            return user;
        }

        String normalizedEmail = profile.email().toLowerCase(Locale.ROOT).trim();
        Optional<User> existingUser = userRepository.findByEmail(normalizedEmail);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            OAuthIdentity identity = new OAuthIdentity(
                    UUID.randomUUID(),
                    user.getId(),
                    provider,
                    profile.providerUserId()
            );
            oauthIdentityRepository.save(identity);
            log.info("[AUTH] Linked {} OAuth identity to existing email: {}", provider, normalizedEmail);
            return user;
        }

        User newUser = new User(
                UUID.randomUUID(),
                normalizedEmail,
                null,
                profile.name() != null ? profile.name().trim() : provider + " User",
                profile.avatarUrl(),
                Instant.now()
        );
        User savedUser = userRepository.save(newUser);

        OAuthIdentity identity = new OAuthIdentity(
                UUID.randomUUID(),
                savedUser.getId(),
                provider,
                profile.providerUserId()
        );
        oauthIdentityRepository.save(identity);

        eventPublisher.publishUserRegistered(new UserRegisteredEvent(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName(),
                Instant.now()
        ));

        log.info("[AUTH] Created new user via {} OAuth: {} (userId: {})", provider, savedUser.getEmail(), savedUser.getId());
        return savedUser;
    }
}
