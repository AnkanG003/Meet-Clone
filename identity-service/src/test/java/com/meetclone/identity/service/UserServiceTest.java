package com.meetclone.identity.service;

import com.meetclone.identity.dto.UserProfileDto;
import com.meetclone.identity.entity.User;
import com.meetclone.identity.event.UserEventPublisher;
import com.meetclone.identity.event.UserUpdatedEvent;
import com.meetclone.identity.exception.UserNotFoundException;
import com.meetclone.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserEventPublisher eventPublisher;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, eventPublisher);
    }

    @Test
    void getProfile_shouldReturnProfileWhenUserExists() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "test@example.com", "hash", "Test User", "https://avatar.png", Instant.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileDto profile = userService.getProfile(userId);

        assertNotNull(profile);
        assertEquals(userId, profile.id());
        assertEquals("test@example.com", profile.email());
        assertEquals("Test User", profile.name());
        assertEquals("https://avatar.png", profile.avatarUrl());
    }

    @Test
    void getProfile_shouldThrowWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getProfile(userId));
    }

    @Test
    void updateProfile_shouldPublishUserUpdatedEvent() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "test@example.com", "hash", "Old Name", null, Instant.now());
        UserProfileDto updateDto = new UserProfileDto(null, null, "New Name", "https://avatar.png");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileDto result = userService.updateProfile(userId, updateDto);

        assertNotNull(result);
        assertEquals("New Name", result.name());
        assertEquals("https://avatar.png", result.avatarUrl());
        verify(userRepository).save(user);
        verify(eventPublisher).publishUserUpdated(any(UserUpdatedEvent.class));
    }
}
