package com.meetclone.identity.security;

import com.meetclone.identity.entity.User;
import org.springframework.stereotype.Component;

import java.security.PublicKey;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    public String generateAccessToken(User user) {
        throw new UnsupportedOperationException("TODO");
    }

    public String generateRefreshToken(User user) {
        throw new UnsupportedOperationException("TODO");
    }

    public boolean validate(String token) {
        throw new UnsupportedOperationException("TODO");
    }

    public UUID getUserId(String token) {
        throw new UnsupportedOperationException("TODO");
    }

    public PublicKey getPublicKey() {
        throw new UnsupportedOperationException("TODO");
    }
}
