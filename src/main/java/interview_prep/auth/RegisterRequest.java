package interview_prep.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 2, max = 80) String username,
        @NotBlank @Size(min = 6, max = 100) String password
) {
}
