package interview_prep.admin;

import interview_prep.auth.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class AdminUserDtos {
    public record UserResponse(
            Long id,
            String email,
            String username,
            UserRole role,
            String avatarUrl,
            Instant createdAt
    ) {
    }

    public record UserUpdateRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 2, max = 80) String username,
            UserRole role
    ) {
    }
}
