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
    private final VerificationCodeService verificationCodes;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserAccountRepository users, AuthSessionRepository sessions,
                       VerificationCodeService verificationCodes) {
        this.users = users;
        this.sessions = sessions;
        this.verificationCodes = verificationCodes;
    }

    @Transactional
    public VerificationStartResponse requestRegistration(RegisterRequest request) {
        String email = verificationCodes.normalizeEmail(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        return verificationCodes.createForRegistration(request, passwordEncoder.encode(request.password()));
    }

    @Transactional
    public AuthResponse confirmRegistration(AuthRequests.ConfirmRegistrationRequest request) {
        EmailVerificationCode code = verificationCodes.consumeEmailCode(
                VerificationPurpose.REGISTRATION,
                request.email(),
                request.code()
        );
        if (users.existsByEmailIgnoreCase(code.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        UserAccount user = users.save(new UserAccount(
                code.getEmail(),
                code.getUsername(),
                code.getPasswordHash()
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

    @Transactional
    public VerificationStartResponse requestPasswordReset(AuthRequests.ForgotPasswordRequest request) {
        return users.findByEmailIgnoreCase(request.email())
                .map(user -> verificationCodes.createForEmail(
                        VerificationPurpose.PASSWORD_RESET,
                        user.getEmail(),
                        "Восстановление пароля"
                ))
                .orElseGet(() -> new VerificationStartResponse(Instant.now()));
    }

    @Transactional
    public void resetPassword(AuthRequests.ResetPasswordRequest request) {
        UserAccount user = users.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid confirmation code"));
        verificationCodes.consumeEmailCode(VerificationPurpose.PASSWORD_RESET, user.getEmail(), request.code());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        users.save(user);
        sessions.deleteByUserId(user.getId());
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
