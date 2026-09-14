package com.meetclone.identity.oauth;

import com.meetclone.identity.dto.OAuthUserProfile;

public interface OAuthProviderHandler {
    String getProviderName();
    String getAuthorizationUrl();
    OAuthUserProfile fetchUserProfile(String authCode);
}
