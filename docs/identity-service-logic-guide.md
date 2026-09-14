# Identity Service — Business Logic Implementation Guide

This guide provides a comprehensive, step-by-step technical manual for implementing the actual business logic in **`identity-service`**.

---

## Architecture & Flow Overview

```
                          ┌───────────────────────┐
                          │    Client / Gateway   │
                          └──────────┬────────────┘
                                     │
                 ┌───────────────────┴───────────────────┐
                 ▼                                       ▼
      POST /auth/register, /login             GET /users/{id}, PATCH /users/{id}
      POST /auth/refresh, /oauth/...          (Protected via JwtAuthFilter)
                 │                                       │
                 ▼                                       ▼
         [AuthController]                        [UserController]
                 │                                       │
        ┌────────┴────────┐                              ▼
        ▼                 ▼                        [UserService]
  [AuthService]     [OAuthService]                       │
        │                 │                              │
        └────────┬────────┴──────────────────────────────┘
                 │
                 ├──► [UserRepository] / [RefreshTokenRepository] / [OAuthIdentityRepository] ──► PostgreSQL
                 │
                 ├──► [JwtTokenProvider] (Signs/Verifies Access & Refresh Tokens)
                 │
                 └──► [UserEventPublisher] ──► Kafka (`user-events` / `user-registered` / `user-updated`)
```

---

## Implementation Order

1. **Step 1: `JwtTokenProvider`** — Token generation, signing, parsing, and claims extraction.
2. **Step 2: `JwtAuthFilter`** — Authorization header interception and `SecurityContext` population.
3. **Step 3: `UserEventPublisher`** — Kafka template event sending with error callbacks.
4. **Step 4: `AuthServiceImpl`** — Registration, password hashing, credential verification, and token rotation.
5. **Step 5: `UserService`** — Profile queries, updates, and mutation event publishing.
6. **Step 6: `OAuthService` & `OAuth2Config`** — Google authorization code exchange, user profile resolution, and identity linking.
7. **Step 7: `GlobalExceptionHandler`** — Mapping errors and validation issues to clean HTTP responses.

---

## 1. `JwtTokenProvider` (`com.meetclone.identity.security`)

### Responsibilities
- Read secret and TTL configuration from `application.yml` (`app.jwt.secret`, `app.jwt.access-token-ttl-minutes`, `app.jwt.refresh-token-ttl-days`).
- Sign JWTs using HMAC-SHA256 (`Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))`).
- Validate signatures and token expiration.
- Extract `userId`, `email`, and other claims from tokens.

### Logic Specifications

#### Key Configuration & Setup
- Injects properties from `@Value("${app.jwt.secret}")`, `@Value("${app.jwt.access-token-ttl-minutes:15}")`, and `@Value("${app.jwt.refresh-token-ttl-days:30}")`.
- Derives a `SecretKey` using `Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))`.

#### Method 1: `generateAccessToken(User user)`
- **Subject (`sub`)**: `user.getId().toString()`
- **Claims**:
  - `"email"`: `user.getEmail()`
  - `"name"`: `user.getName()`
  - `"type"`: `"ACCESS"`
- **IssuedAt**: `new Date()`
- **Expiration**: `new Date(System.currentTimeMillis() + ttlMinutes * 60 * 1000)`
- **Sign with**: Derived `SecretKey` and `Jwts.SIG.HS256`.

#### Method 2: `generateRefreshToken(User user)`
- **Subject (`sub`)**: `user.getId().toString()`
- **Claims**:
  - `"type"`: `"REFRESH"`
- **IssuedAt**: `new Date()`
- **Expiration**: `new Date(System.currentTimeMillis() + ttlDays * 24 * 60 * 60 * 1000)`
- **Sign with**: Derived `SecretKey` and `Jwts.SIG.HS256`.

#### Method 3: `validate(String token)`
- Parse the token using `Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token)`.
- Return `true` if valid.
- Catch `JwtException` / `IllegalArgumentException` and return `false`.

#### Method 4: `getUserId(String token)`
- Parse the claims: `Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload()`.
- Extract subject: `UUID.fromString(claims.getSubject())`.

---

## 2. `JwtAuthFilter` (`com.meetclone.identity.security`)

### Responsibilities
- Intercept all incoming HTTP requests.
- Read the `Authorization` header. If it starts with `Bearer `, extract the token.
- Validate the token with `JwtTokenProvider`.
- If valid, extract `userId` and create a `UsernamePasswordAuthenticationToken` with user details / authorities and set it into `SecurityContextHolder.getContext().setAuthentication(auth)`.
- Continue the filter chain: `chain.doFilter(req, res)`.

### Logic Flow
1. Check if `header == null || !header.startsWith("Bearer ")`:
   - Immediately call `chain.doFilter(req, res)` and return.
2. Extract `token = header.substring(7)`.
3. Check `if (jwtTokenProvider.validate(token))`:
   - Extract `userId = jwtTokenProvider.getUserId(token)`.
   - Create `UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList())`.
   - `authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req))`.
   - `SecurityContextHolder.getContext().setAuthentication(authentication)`.
4. Call `chain.doFilter(req, res)`.

---

## 3. `UserEventPublisher` (`com.meetclone.identity.event`)

### Responsibilities
- Send events to Kafka asynchronously for other microservices (such as `meeting-service` and `notification-service`).

### Topic Strategy
- Topic: `"user-events"` (or separate `"user-registered"` and `"user-updated"` topics).
- Key: `userId.toString()` (guarantees partition ordering per user).

### Logic Specifications

#### `publishUserRegistered(UserRegisteredEvent event)`
```java
kafkaTemplate.send("user-registered", event.userId().toString(), event)
    .whenComplete((result, ex) -> {
        if (ex != null) {
            // log error
        } else {
            // log success
        }
    });
```

#### `publishUserUpdated(UserUpdatedEvent event)`
```java
kafkaTemplate.send("user-updated", event.userId().toString(), event)
    .whenComplete((result, ex) -> {
        if (ex != null) {
            // log error
        }
    });
```

---

## 4. `AuthServiceImpl` (`com.meetclone.identity.service`)

### Responsibilities
Handles user registration, authentication, token issuance, and refresh token rotation.

---

### Method A: `register(RegisterRequest req)`

```
[Register Request]
        │
        ▼
Does Email Exist? ──────── YES ──────► Throw IllegalArgumentException("Email already in use")
        │
        NO
        │
        ▼
Hash Password (BCrypt)
        │
        ▼
Create & Save User (users table)
        │
        ▼
Generate Access Token + Refresh Token
        │
        ▼
Save Refresh Token (refresh_tokens table)
        │
        ▼
Publish UserRegisteredEvent to Kafka
        │
        ▼
Return LoginResponse(accessToken, refreshToken)
```

#### Step-by-Step Implementation
1. Check if user exists:
   ```java
   if (userRepository.findByEmail(request.email().toLowerCase().trim()).isPresent()) {
       throw new IllegalArgumentException("Email is already registered");
   }
   ```
2. Hash password:
   ```java
   String passwordHash = passwordEncoder.encode(request.password());
   ```
3. Build & Save User:
   ```java
   User user = new User(
       UUID.randomUUID(),
       request.email().toLowerCase().trim(),
       passwordHash,
       request.name().trim(),
       null, // avatarUrl
       Instant.now()
   );
   User savedUser = userRepository.save(user);
   ```
4. Generate Tokens:
   ```java
   String accessToken = jwtTokenProvider.generateAccessToken(savedUser);
   String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser);
   ```
5. Store Refresh Token:
   ```java
   RefreshToken tokenEntity = new RefreshToken(
       UUID.randomUUID(),
       refreshToken,
       savedUser.getId(),
       Instant.now().plus(30, ChronoUnit.DAYS),
       false
   );
   refreshTokenRepository.save(tokenEntity);
   ```
6. Publish Kafka Event:
   ```java
   eventPublisher.publishUserRegistered(new UserRegisteredEvent(
       savedUser.getId(),
       savedUser.getEmail(),
       savedUser.getName(),
       Instant.now()
   ));
   ```
7. Return `new LoginResponse(accessToken, refreshToken)`.

---

### Method B: `login(LoginRequest req)`

```
[Login Request]
        │
        ▼
Find User by Email ────── NOT FOUND ──► Throw InvalidCredentialsException("Invalid email or password")
        │
      FOUND
        │
        ▼
Password Matches? ─────── NO ─────────► Throw InvalidCredentialsException("Invalid email or password")
        │
       YES
        │
        ▼
Generate Access Token + Refresh Token
        │
        ▼
Revoke/Delete Old Refresh Tokens & Save New Refresh Token
        │
        ▼
Return LoginResponse(accessToken, refreshToken)
```

#### Step-by-Step Implementation
1. Query user:
   ```java
   User user = userRepository.findByEmail(request.email().toLowerCase().trim())
       .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
   ```
2. Check password:
   ```java
   if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
       throw new InvalidCredentialsException("Invalid email or password");
   }
   ```
3. Generate tokens:
   ```java
   String accessToken = jwtTokenProvider.generateAccessToken(user);
   String refreshToken = jwtTokenProvider.generateRefreshToken(user);
   ```
4. Save new refresh token:
   ```java
   RefreshToken tokenEntity = new RefreshToken(
       UUID.randomUUID(),
       refreshToken,
       user.getId(),
       Instant.now().plus(30, ChronoUnit.DAYS),
       false
   );
   refreshTokenRepository.save(tokenEntity);
   ```
5. Return `new LoginResponse(accessToken, refreshToken)`.

---

### Method C: `refresh(String refreshToken)`

```
[Refresh Request]
        │
        ▼
Validate Token Signature & Expiry (JwtTokenProvider) ──── INVALID ──► Throw InvalidCredentialsException
        │
      VALID
        │
        ▼
Find in refresh_tokens table ──────────────────────── NOT FOUND ──► Throw InvalidCredentialsException
        │
      FOUND
        │
        ▼
Is Revoked or Expired? ───────────────────────────────── YES ─────► Throw InvalidCredentialsException
        │
        NO
        │
        ▼
Revoke/Delete Old Token (Rotation)
        │
        ▼
Fetch User -> Generate New Access Token & New Refresh Token
        │
        ▼
Save New Refresh Token
        │
        ▼
Return LoginResponse(newAccessToken, newRefreshToken)
```

#### Step-by-Step Implementation
1. Find token in DB:
   ```java
   RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
       .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));
   ```
2. Check revocation & expiry:
   ```java
   if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(Instant.now())) {
       throw new InvalidCredentialsException("Refresh token is expired or revoked");
   }
   ```
3. Load User:
   ```java
   User user = userRepository.findById(storedToken.getUserId())
       .orElseThrow(() -> new UserNotFoundException("User not found"));
   ```
4. Rotate Token (mark old revoked or delete):
   ```java
   storedToken.setRevoked(true);
   refreshTokenRepository.save(storedToken);
   ```
5. Issue new tokens:
   ```java
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
   ```
6. Return `new LoginResponse(newAccessToken, newRefreshToken)`.

---

## 5. `UserService` (`com.meetclone.identity.service`)

### Responsibilities
User profile retrieval and updates.

### Method A: `getProfile(UUID userId)`
1. Query database:
   ```java
   User user = userRepository.findById(userId)
       .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
   ```
2. Return DTO:
   ```java
   return new UserProfileDto(user.getId(), user.getEmail(), user.getName(), user.getAvatarUrl());
   ```

### Method B: `updateProfile(UUID userId, UserProfileDto update)`
1. Query user:
   ```java
   User user = userRepository.findById(userId)
       .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
   ```
2. Update mutable fields:
   ```java
   if (update.name() != null && !update.name().isBlank()) {
       user.setName(update.name().trim());
   }
   if (update.avatarUrl() != null) {
       user.setAvatarUrl(update.avatarUrl().trim());
   }
   User saved = userRepository.save(user);
   ```
3. Publish Kafka event for other services:
   ```java
   eventPublisher.publishUserUpdated(new UserUpdatedEvent(
       saved.getId(),
       saved.getName(),
       saved.getAvatarUrl(),
       Instant.now()
   ));
   ```
4. Return `new UserProfileDto(saved.getId(), saved.getEmail(), saved.getName(), saved.getAvatarUrl())`.

---

## 6. `OAuthService` (`com.meetclone.identity.service`)

### Responsibilities
Handles Google OAuth2 callback code exchange, user creation/linking, and issuing JWT credentials.

```
[Google Auth Code]
        │
        ▼
Exchange Code for Tokens at Google Token Endpoint
(`https://oauth2.googleapis.com/token`)
        │
        ▼
Fetch Google User Profile (`sub`, `email`, `name`, `picture`)
        │
        ▼
Find OAuthIdentity by provider="google" & providerUserId=sub
        │
  ┌─────┴─────────────────────────────────┐
  ▼                                       ▼
EXISTS                               NOT FOUND
  │                                       │
  │                         Does User with Email exist?
  │                               ┌───────┴────────┐
  │                               ▼                ▼
  │                             EXISTS         NOT FOUND
  │                               │                │
  │                     Link OAuthIdentity    Create User & Link
  │                               │                │
  └───────────────────────────────┼────────────────┘
                                  ▼
                Issue Access Token + Refresh Token
                                  ▼
                Return LoginResponse(access, refresh)
```

---

## 7. Security Context & Principal Helpers

In `UserController`, to get the current authenticated user's ID:
```java
@GetMapping("/me")
public UserProfileDto getCurrentUser(@AuthenticationPrincipal UUID currentUserId) {
    return userService.getProfile(currentUserId);
}
```

Or extract from `SecurityContextHolder`:
```java
UUID currentUserId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
```

---

## 8. Summary Checklist Before Running

- [ ] `JwtTokenProvider`: Secret key derived safely, signing and parsing HS256 tokens.
- [ ] `JwtAuthFilter`: Intercepts `Authorization: Bearer <token>`, sets `SecurityContext`.
- [ ] `AuthServiceImpl`: `register`, `login`, and `refresh` properly hashing passwords, rotating tokens, and publishing `UserRegisteredEvent`.
- [ ] `UserService`: `getProfile` and `updateProfile` updating data and publishing `UserUpdatedEvent`.
- [ ] `UserEventPublisher`: Successfully sends to Kafka topics.
- [ ] `GlobalExceptionHandler`: Returns JSON error bodies with appropriate HTTP status codes (400, 401, 404).
