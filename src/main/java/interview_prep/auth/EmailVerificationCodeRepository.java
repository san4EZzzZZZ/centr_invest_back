package interview_prep.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {
    Optional<EmailVerificationCode> findTopByPurposeAndEmailIgnoreCaseAndConsumedAtIsNullOrderByCreatedAtDesc(
            VerificationPurpose purpose, String email);

    Optional<EmailVerificationCode> findFirstByPurposeAndUser_IdAndConsumedAtIsNullOrderByCreatedAtDesc(
            VerificationPurpose purpose, Long userId);

    List<EmailVerificationCode> findByPurposeAndEmailIgnoreCaseAndConsumedAtIsNull(
            VerificationPurpose purpose, String email);

    List<EmailVerificationCode> findByPurposeAndUser_IdAndConsumedAtIsNull(
            VerificationPurpose purpose, Long userId);
}
