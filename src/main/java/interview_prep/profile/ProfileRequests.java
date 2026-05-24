package interview_prep.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProfileRequests {
    public record UpdateUsernameRequest(@NotBlank @Size(min = 2, max = 80) String username) {
    }

    public record RequestEmailChangeRequest(@Email @NotBlank String newEmail) {
    }

    public record ConfirmEmailChangeRequest(@Email @NotBlank String newEmail, @NotBlank String code) {
    }

    public record ConfirmPasswordChangeRequest(
            @NotBlank String code,
            @NotBlank @Size(min = 6, max = 100) String newPassword
    ) {
    }
}
