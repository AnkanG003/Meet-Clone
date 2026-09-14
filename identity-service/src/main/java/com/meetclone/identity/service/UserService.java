package com.meetclone.identity.service;

import com.meetclone.identity.dto.UserProfileDto;
import com.meetclone.identity.entity.User;
import com.meetclone.identity.event.UserEventPublisher;
import com.meetclone.identity.event.UserUpdatedEvent;
import com.meetclone.identity.exception.UserNotFoundException;
import com.meetclone.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;

    public UserService(UserRepository userRepository, UserEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        return new UserProfileDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getAvatarUrl()
        );
    }

    @Transactional
    public UserProfileDto updateProfile(UUID userId, UserProfileDto update) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        if (update.name() != null && !update.name().isBlank()) {
            user.setName(update.name().trim());
        }
        if (update.avatarUrl() != null) {
            user.setAvatarUrl(update.avatarUrl().trim());
        }

        User savedUser = userRepository.save(user);

        eventPublisher.publishUserUpdated(new UserUpdatedEvent(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getAvatarUrl(),
                Instant.now()
        ));

        return new UserProfileDto(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName(),
                savedUser.getAvatarUrl()
        );
    }
}
