package interview_prep.auth;

import java.time.Instant;

public record AuthResponse(String token, Instant expiresAt, CurrentUser user) {
}
