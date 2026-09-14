package com.meetclone.identity.service;

import org.junit.jupiter.api.Test;

class AuthServiceTest {
    @Test
    void register_shouldHashPasswordAndSaveUser() {}

    @Test
    void register_shouldThrowWhenEmailAlreadyExists() {}

    @Test
    void login_shouldIssueTokensOnValidCredentials() {}

    @Test
    void login_shouldThrowOnInvalidPassword() {}

    @Test
    void refresh_shouldRotateToken() {}
}
