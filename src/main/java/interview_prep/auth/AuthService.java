package interview_prep.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class AuthService {
    private final UserAccountRepository users;
    private final AuthSessionRepository sessions;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserAccountRepository users, AuthSessionRepository sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        UserAccount user = users.save(new UserAccount(
                request.email().trim().toLowerCase(),
                request.username().trim(),
                passwordEncoder.encode(request.password())
        ));
        return createSession(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserAccount user = users.findByEmailIgnoreCase(request.email())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        return createSession(user);
    }

    @Transactional
    public void logout(String token) {
        sessions.deleteByToken(token);
    }

    private AuthResponse createSession(UserAccount user) {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now().plus(14, ChronoUnit.DAYS);
        sessions.save(new AuthSession(token, user, expiresAt));
        return new AuthResponse(token, expiresAt, CurrentUser.from(user));
    }
}
