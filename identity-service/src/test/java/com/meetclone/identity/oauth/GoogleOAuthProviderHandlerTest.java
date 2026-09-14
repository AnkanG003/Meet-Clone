package com.meetclone.identity.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

class GoogleOAuthProviderHandlerTest {

    private GoogleOAuthProviderHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GoogleOAuthProviderHandler(
                RestClient.builder().build(),
                "test-client-id",
                "test-client-secret",
                "http://localhost:8081/auth/oauth/google/callback"
        );
    }

    @Test
    void getProviderName_shouldBeGoogle() {
        assertEquals("GOOGLE", handler.getProviderName());
    }

    @Test
    void getAuthorizationUrl_shouldContainGoogleOAuthParams() {
        String url = handler.getAuthorizationUrl();

        assertNotNull(url);
        assertTrue(url.contains("accounts.google.com"));
        assertTrue(url.contains("client_id=test-client-id"));
        assertTrue(url.contains("redirect_uri="));
        assertTrue(url.contains("response_type=code"));
    }
}
