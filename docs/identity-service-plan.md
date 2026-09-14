# identity-service — Build Plan

Goal: get the service compiling, running, and wired end-to-end (routes → services → repos → DB → Kafka) with **empty method bodies / TODOs** so you write the actual logic yourself. No business logic included.

---

## Build order (do it in this sequence)

1. `pom.xml` — dependencies
2. `application.yml` + Flyway migration — DB/Kafka/JWT config exists before code needs it
3. `entity/` — JPA models (fields only)
4. `repository/` — interfaces (just `extends JpaRepository`)
5. `dto/` — request/response shapes
6. `exception/` + `GlobalExceptionHandler` — so services can throw meaningfully from day 1
7. `security/` — JWT provider + filter skeletons, `SecurityConfig`
8. `event/` — Kafka payload records + publisher skeleton
9. `service/` — interfaces/classes with empty methods (this is where you'll write logic)
10. `controller/` — endpoints wired to service calls
11. `config/OAuth2Config`, `KafkaProducerConfig`
12. Tests — empty test classes with method names stubbed, run `mvn spring-boot:run` to confirm boot

---

## 1. `pom.xml` dependencies

```xml
<dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-oauth2-client</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.springframework.kafka</groupId><artifactId>spring-kafka</artifactId></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>0.12.5</version></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>0.12.5</version><scope>runtime</scope></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>0.12.5</version><scope>runtime</scope></dependency>
    <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-database-postgresql</artifactId></dependency>
    <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>

    <!-- test -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.springframework.kafka</groupId><artifactId>spring-kafka-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.testcontainers</groupId><artifactId>postgresql</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.testcontainers</groupId><artifactId>junit-jupiter</artifactId><scope>test</scope></dependency>
</dependencies>
```

---

## 2. `application.yml`

```yaml
server:
  port: 8081

spring:
  application:
    name: identity-service
  datasource:
    url: jdbc:postgresql://localhost:5432/identity_db
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: validate   # Flyway owns schema
    show-sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

app:
  jwt:
    secret: ${JWT_SECRET:change-me-to-a-long-random-string}
    access-token-ttl-minutes: 15
    refresh-token-ttl-days: 30
  oauth2:
    google:
      client-id: ${GOOGLE_CLIENT_ID:}
      client-secret: ${GOOGLE_CLIENT_SECRET:}
```

## `db/migration/V1__init.sql`

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    name VARCHAR(255),
    avatar_url VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE oauth_identities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    provider VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    UNIQUE (provider, provider_user_id)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(512) UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT false
);
```

---

## 3. `entity/`

```java
// entity/User.java
@Entity @Table(name = "users") @Getter @Setter
public class User {
    @Id @GeneratedValue private UUID id;
    @Column(unique = true, nullable = false) private String email;
    private String passwordHash;
    private String name;
    private String avatarUrl;
    @Column(nullable = false, updatable = false) private Instant createdAt;
}

// entity/OAuthIdentity.java
@Entity @Table(name = "oauth_identities") @Getter @Setter
public class OAuthIdentity {
    @Id @GeneratedValue private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    private String provider;
    private String providerUserId;
}

// entity/RefreshToken.java
@Entity @Table(name = "refresh_tokens") @Getter @Setter
public class RefreshToken {
    @Id @GeneratedValue private UUID id;
    @Column(unique = true, nullable = false) private String token;
    @Column(name = "user_id", nullable = false) private UUID userId;
    private Instant expiresAt;
    private boolean revoked;
}
```

---

## 4. `repository/`

```java
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentity, UUID> {
    Optional<OAuthIdentity> findByProviderAndProviderUserId(String provider, String providerUserId);
}

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUserId(UUID userId);
}
```

---

## 5. `dto/`

```java
public record RegisterRequest(@Email String email, @NotBlank String password, @NotBlank String name) {}

public record LoginRequest(@Email String email, @NotBlank String password) {}
public record LoginResponse(String accessToken, String refreshToken) {}

public record UserProfileDto(UUID id, String email, String name, String avatarUrl) {}
```

---

## 6. `exception/`

```java
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String msg) { super(msg); }
}

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String msg) { super(msg); }
}

@ControllerAdvice
public class GlobalExceptionHandler {
    // TODO: @ExceptionHandler for InvalidCredentialsException -> 401
    // TODO: @ExceptionHandler for UserNotFoundException -> 404
    // TODO: @ExceptionHandler for MethodArgumentNotValidException -> 400
}
```

---

## 7. `security/`

```java
@Component
public class JwtTokenProvider {
    public String generateAccessToken(User user) { throw new UnsupportedOperationException("TODO"); }
    public String generateRefreshToken(User user) { throw new UnsupportedOperationException("TODO"); }
    public boolean validate(String token) { throw new UnsupportedOperationException("TODO"); }
    public UUID getUserId(String token) { throw new UnsupportedOperationException("TODO"); }
    public PublicKey getPublicKey() { throw new UnsupportedOperationException("TODO"); } // other services will need this
}

public class JwtAuthFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        // TODO: extract Authorization header, validate, set SecurityContext
        chain.doFilter(req, res);
    }
}

@Configuration
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // TODO: permitAll on /auth/**, authenticated elsewhere, add JwtAuthFilter, CORS config
        return http.build();
    }
}
```

---

## 8. `event/`

```java
public record UserRegisteredEvent(UUID userId, String email, String name, Instant occurredAt) {}
public record UserUpdatedEvent(UUID userId, String name, String avatarUrl, Instant occurredAt) {}

@Component @RequiredArgsConstructor
public class UserEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    public void publishUserRegistered(UserRegisteredEvent event) { /* TODO: kafkaTemplate.send("user-registered", ...) */ }
    public void publishUserUpdated(UserUpdatedEvent event) { /* TODO: kafkaTemplate.send("user-updated", ...) */ }
}
```

---

## 9. `service/` — write your logic here

```java
public interface AuthService {
    LoginResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    LoginResponse refresh(String refreshToken);
}

@Service @RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserEventPublisher eventPublisher;

    @Override public LoginResponse register(RegisterRequest request) { throw new UnsupportedOperationException("TODO"); }
    @Override public LoginResponse login(LoginRequest request) { throw new UnsupportedOperationException("TODO"); }
    @Override public LoginResponse refresh(String refreshToken) { throw new UnsupportedOperationException("TODO"); }
}

@Service @RequiredArgsConstructor
public class OAuthService {
    public LoginResponse handleGoogleCallback(String authCode) { throw new UnsupportedOperationException("TODO"); }
}

@Service @RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;

    public UserProfileDto getProfile(UUID userId) { throw new UnsupportedOperationException("TODO"); }
    public UserProfileDto updateProfile(UUID userId, UserProfileDto update) { throw new UnsupportedOperationException("TODO"); }
}
```

---

## 10. `controller/`

```java
@RestController @RequestMapping("/auth") @RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register") public LoginResponse register(@Valid @RequestBody RegisterRequest req) { return authService.register(req); }
    @PostMapping("/login") public LoginResponse login(@Valid @RequestBody LoginRequest req) { return authService.login(req); }
    @PostMapping("/refresh") public LoginResponse refresh(@RequestParam String refreshToken) { return authService.refresh(refreshToken); }
    @GetMapping("/oauth/google/callback") public LoginResponse googleCallback(@RequestParam String code) { throw new UnsupportedOperationException("TODO: call OAuthService"); }
}

@RestController @RequestMapping("/users") @RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/{id}") public UserProfileDto get(@PathVariable UUID id) { return userService.getProfile(id); }
    @PatchMapping("/{id}") public UserProfileDto update(@PathVariable UUID id, @RequestBody UserProfileDto dto) { return userService.updateProfile(id, dto); }
}
```

---

## 11. `config/`

```java
@Configuration
public class OAuth2Config {
    // TODO: bind app.oauth2.google.* properties, expose a ClientRegistrationRepository or a plain RestTemplate-based exchanger
}

@Configuration
public class KafkaProducerConfig {
    @Bean
    public ProducerFactory<String, Object> producerFactory() { throw new UnsupportedOperationException("TODO"); }
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() { throw new UnsupportedOperationException("TODO"); }
}
```

---

## 12. Test skeletons (just method names, no assertions yet)

```java
class AuthServiceTest {
    @Test void register_shouldHashPasswordAndSaveUser() {}
    @Test void register_shouldThrowWhenEmailAlreadyExists() {}
    @Test void login_shouldIssueTokensOnValidCredentials() {}
    @Test void login_shouldThrowOnInvalidPassword() {}
    @Test void refresh_shouldRotateToken() {}
}

class UserServiceTest {
    @Test void updateProfile_shouldPublishUserUpdatedEvent() {}
}

@SpringBootTest @AutoConfigureMockMvc
class AuthControllerIT {
    // TODO: @Testcontainers Postgres, register/login happy path via MockMvc
}
```

---

## Checklist to confirm the service is "usable" before writing logic

- [ ] `mvn clean install` compiles with all the above stubs (the `UnsupportedOperationException` throws are expected — they compile fine)
- [ ] Postgres running locally, `flyway migrate` (or app boot) applies `V1__init.sql` cleanly
- [ ] `mvn spring-boot:run` boots without bean-wiring errors
- [ ] `POST /auth/register` returns a (currently 500, because TODO) response instead of 404 — confirms routing works
- [ ] Kafka topic `user-registered` reachable (local Kafka or skip until you implement the publisher)

Once this boots clean, fill in the `TODO`s inside `AuthServiceImpl`, `UserService`, `JwtTokenProvider`, `JwtAuthFilter`, `SecurityConfig`, and `OAuth2Config` — that's the actual logic, and everything around it is already wired.
