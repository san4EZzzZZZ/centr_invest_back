package interview_prep.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
public class VerificationCodeService {
    private final EmailVerificationCodeRepository codes;
    private final MailService mailService;
    private final BCryptPasswordEncoder encoder;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Duration ttl;

    public VerificationCodeService(EmailVerificationCodeRepository codes, MailService mailService,
                                   BCryptPasswordEncoder encoder,
                                   @Value("${app.mail.code-ttl}") Duration ttl) {
        this.codes = codes;
        this.mailService = mailService;
        this.encoder = encoder;
        this.ttl = ttl;
    }

    @Transactional
    public VerificationStartResponse createForEmail(VerificationPurpose purpose, String email, String subject) {
        String normalizedEmail = normalizeEmail(email);
        consumeOldEmailCodes(purpose, normalizedEmail);
        String code = generateCode();
        EmailVerificationCode saved = codes.save(new EmailVerificationCode(
                purpose,
                null,
                normalizedEmail,
                null,
                null,
                null,
                encoder.encode(code),
                Instant.now().plus(ttl)
        ));
        mailService.sendCode(normalizedEmail, subject, code);
        return new VerificationStartResponse(saved.getExpiresAt());
    }

    @Transactional
    public VerificationStartResponse createForRegistration(RegisterRequest request, String passwordHash) {
        String email = normalizeEmail(request.email());
        consumeOldEmailCodes(VerificationPurpose.REGISTRATION, email);
        String code = generateCode();
        EmailVerificationCode saved = codes.save(new EmailVerificationCode(
                VerificationPurpose.REGISTRATION,
                null,
                email,
                null,
                request.username().trim(),
                passwordHash,
                encoder.encode(code),
                Instant.now().plus(ttl)
        ));
        mailService.sendCode(email, "Подтверждение регистрации", code);
        return new VerificationStartResponse(saved.getExpiresAt());
    }

    @Transactional
    public VerificationStartResponse createForUser(VerificationPurpose purpose, UserAccount user, String newEmail,
                                                   String subject) {
        consumeOldUserCodes(purpose, user.getId());
        String code = generateCode();
        EmailVerificationCode saved = codes.save(new EmailVerificationCode(
                purpose,
                user,
                user.getEmail(),
                newEmail == null ? null : normalizeEmail(newEmail),
                null,
                null,
                encoder.encode(code),
                Instant.now().plus(ttl)
        ));
        mailService.sendCode(newEmail == null ? user.getEmail() : normalizeEmail(newEmail), subject, code);
        return new VerificationStartResponse(saved.getExpiresAt());
    }

    @Transactional
    public EmailVerificationCode consumeEmailCode(VerificationPurpose purpose, String email, String code) {
        EmailVerificationCode found = codes
                .findTopByPurposeAndEmailIgnoreCaseAndConsumedAtIsNullOrderByCreatedAtDesc(purpose, normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("Confirmation code was not requested"));
        return consume(found, code);
    }

    @Transactional
    public EmailVerificationCode consumeUserCode(VerificationPurpose purpose, Long userId, String code) {
        EmailVerificationCode found = codes
                .findFirstByPurposeAndUser_IdAndConsumedAtIsNullOrderByCreatedAtDesc(purpose, userId)
                .orElseThrow(() -> new IllegalArgumentException("Confirmation code was not requested"));
        return consume(found, code);
    }

    public String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private EmailVerificationCode consume(EmailVerificationCode found, String code) {
        if (found.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Confirmation code expired");
        }
        if (code == null || !encoder.matches(code.trim(), found.getCodeHash())) {
            throw new IllegalArgumentException("Invalid confirmation code");
        }
        found.setConsumedAt(Instant.now());
        return codes.save(found);
    }

    private void consumeOldEmailCodes(VerificationPurpose purpose, String email) {
        codes.findByPurposeAndEmailIgnoreCaseAndConsumedAtIsNull(purpose, email).forEach(code -> {
            code.setConsumedAt(Instant.now());
            codes.save(code);
        });
    }

    private void consumeOldUserCodes(VerificationPurpose purpose, Long userId) {
        codes.findByPurposeAndUser_IdAndConsumedAtIsNull(purpose, userId).forEach(code -> {
            code.setConsumedAt(Instant.now());
            codes.save(code);
        });
    }

    private String generateCode() {
        return "%06d".formatted(secureRandom.nextInt(1_000_000));
    }
}
