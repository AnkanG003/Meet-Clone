package com.meetclone.identity.controller;

import com.meetclone.identity.dto.UserProfileDto;
import com.meetclone.identity.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public UserProfileDto get(@PathVariable UUID id) {
        return userService.getProfile(id);
    }

    @PatchMapping("/{id}")
    public UserProfileDto update(@PathVariable UUID id, @RequestBody UserProfileDto dto) {
        return userService.updateProfile(id, dto);
    }
}
