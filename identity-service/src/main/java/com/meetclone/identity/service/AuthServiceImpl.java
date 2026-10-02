package com.meetclone.identity.service;

import com.meetclone.identity.dto.LoginRequest;
import com.meetclone.identity.dto.LoginResponse;
import com.meetclone.identity.dto.RegisterRequest;
import com.meetclone.identity.entity.RefreshToken;
import com.meetclone.identity.entity.User;
import com.meetclone.identity.event.UserEventPublisher;
import com.meetclone.identity.event.UserRegisteredEvent;
import com.meetclone.identity.exception.InvalidCredentialsException;
import com.meetclone.identity.exception.UserNotFoundException;
import com.meetclone.identity.repository.RefreshTokenRepository;
import com.meetclone.identity.repository.UserRepository;
import com.meetclone.identity.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserEventPublisher eventPublisher;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            UserEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(
                UUID.randomUUID(),
                email,
                passwordHash,
                request.name().trim(),
                null,
                Instant.now()
        );
        User savedUser = userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(savedUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser);

        RefreshToken tokenEntity = new RefreshToken(
                UUID.randomUUID(),
                refreshToken,
                savedUser.getId(),
                Instant.now().plus(30, ChronoUnit.DAYS),
                false
        );
        refreshTokenRepository.save(tokenEntity);

        eventPublisher.publishUserRegistered(new UserRegisteredEvent(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName(),
                Instant.now()
        ));

        log.info("[AUTH] User successfully registered: {} (userId: {})", savedUser.getEmail(), savedUser.getId());

        return new LoginResponse(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.email().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        RefreshToken tokenEntity = new RefreshToken(
                UUID.randomUUID(),
                refreshToken,
                user.getId(),
                Instant.now().plus(30, ChronoUnit.DAYS),
                false
        );
        refreshTokenRepository.save(tokenEntity);

        log.info("[AUTH] User successfully logged in: {} (userId: {})", user.getEmail(), user.getId());

        return new LoginResponse(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public LoginResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token not found"));

        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidCredentialsException("Refresh token is expired or revoked");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + storedToken.getUserId()));

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        RefreshToken newTokenEntity = new RefreshToken(
                UUID.randomUUID(),
                newRefreshToken,
                user.getId(),
                Instant.now().plus(30, ChronoUnit.DAYS),
                false
        );
        refreshTokenRepository.save(newTokenEntity);

        log.info("[AUTH] Token successfully rotated for userId: {}", user.getId());

        return new LoginResponse(newAccessToken, newRefreshToken);
    }
}
