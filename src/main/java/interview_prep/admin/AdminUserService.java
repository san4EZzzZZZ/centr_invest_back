package interview_prep.admin;

import interview_prep.auth.CurrentUser;
import interview_prep.auth.UserAccount;
import interview_prep.auth.UserAccountRepository;
import interview_prep.auth.UserRole;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUserService {
    private final UserAccountRepository users;

    public AdminUserService(UserAccountRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<AdminUserDtos.UserResponse> list(String search) {
        String query = search == null ? null : search.trim();
        List<UserAccount> found = query == null || query.isEmpty()
                ? users.findAllByOrderByCreatedAtDesc()
                : users.findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCaseOrderByCreatedAtDesc(query, query);
        return found.stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdminUserDtos.UserResponse update(Long userId, AdminUserDtos.UserUpdateRequest request) {
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Super admin cannot be edited here");
        }
        UserRole role = request.role() == null ? user.getRole() : request.role();
        if (role == UserRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Cannot grant SUPER_ADMIN role from API");
        }

        String email = request.email().trim().toLowerCase();
        users.findByEmailIgnoreCase(email)
                .filter(found -> !found.getId().equals(userId))
                .ifPresent(found -> {
                    throw new IllegalArgumentException("Email is already registered");
                });

        user.setEmail(email);
        user.setUsername(request.username().trim());
        user.setRole(role);
        return toResponse(users.save(user));
    }

    private AdminUserDtos.UserResponse toResponse(UserAccount user) {
        CurrentUser current = CurrentUser.from(user);
        return new AdminUserDtos.UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole(),
                current.avatarUrl(),
                user.getCreatedAt()
        );
    }
}
