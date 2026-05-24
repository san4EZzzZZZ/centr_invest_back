package interview_prep.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthRequests {
    public record ConfirmRegistrationRequest(
            @Email @NotBlank String email,
            @NotBlank String code
    ) {
    }

    public record ForgotPasswordRequest(@Email @NotBlank String email) {
    }

    public record ResetPasswordRequest(
            @Email @NotBlank String email,
            @NotBlank String code,
            @NotBlank @Size(min = 6, max = 100) String newPassword
    ) {
    }
}
