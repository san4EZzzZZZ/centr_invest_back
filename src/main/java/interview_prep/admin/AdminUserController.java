package interview_prep.admin;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminGuard adminGuard;
    private final AdminUserService adminUserService;

    public AdminUserController(AdminGuard adminGuard, AdminUserService adminUserService) {
        this.adminGuard = adminGuard;
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public List<AdminUserDtos.UserResponse> list(@RequestParam(required = false) String search) {
        adminGuard.requireSuperAdmin();
        return adminUserService.list(search);
    }

    @PatchMapping("/{userId}")
    public AdminUserDtos.UserResponse update(@PathVariable Long userId,
                                             @Valid @RequestBody AdminUserDtos.UserUpdateRequest request) {
        adminGuard.requireSuperAdmin();
        return adminUserService.update(userId, request);
    }
}
