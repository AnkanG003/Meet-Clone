package com.meetclone.identity.oauth;

import com.meetclone.identity.dto.GoogleTokenResponse;
import com.meetclone.identity.dto.GoogleUserInfo;
import com.meetclone.identity.dto.OAuthUserProfile;
import com.meetclone.identity.exception.InvalidCredentialsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class GoogleOAuthProviderHandler implements OAuthProviderHandler {

    public static final String PROVIDER_NAME = "GOOGLE";
    private static final String GOOGLE_TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URI = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public GoogleOAuthProviderHandler(
            RestClient restClient,
            @Value("${app.oauth2.google.client-id}") String clientId,
            @Value("${app.oauth2.google.client-secret}") String clientSecret,
            @Value("${app.oauth2.google.redirect-uri:http://localhost:8081/auth/oauth/google/callback}") String redirectUri
    ) {
        this.restClient = restClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String getAuthorizationUrl() {
        return "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&scope=" + URLEncoder.encode("openid profile email", StandardCharsets.UTF_8) +
                "&access_type=offline" +
                "&prompt=consent";
    }

    @Override
    public OAuthUserProfile fetchUserProfile(String authCode) {
        GoogleTokenResponse tokenResponse = exchangeAuthCodeForTokens(authCode);
        if (tokenResponse == null || tokenResponse.accessToken() == null) {
            throw new InvalidCredentialsException("Failed to obtain access token from Google");
        }

        GoogleUserInfo userInfo = fetchGoogleUserInfo(tokenResponse.accessToken());
        if (userInfo == null || userInfo.email() == null || userInfo.sub() == null) {
            throw new InvalidCredentialsException("Failed to retrieve user profile from Google");
        }

        return new OAuthUserProfile(
                userInfo.sub(),
                userInfo.email(),
                userInfo.name(),
                userInfo.picture()
        );
    }

    private GoogleTokenResponse exchangeAuthCodeForTokens(String authCode) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", authCode);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        try {
            return restClient.post()
                    .uri(GOOGLE_TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(GoogleTokenResponse.class);
        } catch (Exception e) {
            throw new InvalidCredentialsException("Error exchanging Google authorization code: " + e.getMessage());
        }
    }

    private GoogleUserInfo fetchGoogleUserInfo(String googleAccessToken) {
        try {
            return restClient.get()
                    .uri(GOOGLE_USER_INFO_URI)
                    .header("Authorization", "Bearer " + googleAccessToken)
                    .retrieve()
                    .body(GoogleUserInfo.class);
        } catch (Exception e) {
            throw new InvalidCredentialsException("Error fetching user info from Google: " + e.getMessage());
        }
    }
}
