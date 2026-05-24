package interview_prep.admin;

import interview_prep.auth.CurrentUserContext;
import interview_prep.auth.ForbiddenException;
import interview_prep.auth.UserAccount;
import interview_prep.auth.UserRole;
import org.springframework.stereotype.Component;

@Component
public class AdminGuard {
    public UserAccount requireAdmin() {
        UserAccount user = CurrentUserContext.getRequired();
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.SUPER_ADMIN) {
            throw new ForbiddenException("Admin role required");
        }
        return user;
    }

    public UserAccount requireSuperAdmin() {
        UserAccount user = CurrentUserContext.getRequired();
        if (user.getRole() != UserRole.SUPER_ADMIN) {
            throw new ForbiddenException("Super admin role required");
        }
        return user;
    }
}
