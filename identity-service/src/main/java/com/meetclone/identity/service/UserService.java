package com.meetclone.identity.service;

import com.meetclone.identity.dto.UserProfileDto;
import com.meetclone.identity.event.UserEventPublisher;
import com.meetclone.identity.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;

    public UserService(UserRepository userRepository, UserEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    public UserProfileDto getProfile(UUID userId) {
        throw new UnsupportedOperationException("TODO");
    }

    public UserProfileDto updateProfile(UUID userId, UserProfileDto update) {
        throw new UnsupportedOperationException("TODO");
    }
}
