package interview_prep.auth;

import java.time.Instant;

public record VerificationStartResponse(Instant expiresAt) {
}
