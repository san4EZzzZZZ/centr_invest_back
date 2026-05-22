package interview_prep.admin;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tests")
public class AdminTestController {
    private final AdminGuard adminGuard;
    private final AdminTestService adminTestService;

    public AdminTestController(AdminGuard adminGuard, AdminTestService adminTestService) {
        this.adminGuard = adminGuard;
        this.adminTestService = adminTestService;
    }

    @GetMapping
    public List<AdminDtos.TestSummaryResponse> list() {
        adminGuard.requireAdmin();
        return adminTestService.list();
    }

    @GetMapping("/{testId}")
    public AdminDtos.TestDetailsResponse get(@PathVariable Long testId) {
        adminGuard.requireAdmin();
        return adminTestService.get(testId);
    }

    @PostMapping
    public AdminDtos.TestDetailsResponse create(@Valid @RequestBody AdminDtos.TestUpsertRequest request) {
        adminGuard.requireAdmin();
        return adminTestService.create(request);
    }

    @PutMapping("/{testId}")
    public AdminDtos.TestDetailsResponse update(@PathVariable Long testId,
                                                @Valid @RequestBody AdminDtos.TestUpsertRequest request) {
        adminGuard.requireAdmin();
        return adminTestService.update(testId, request);
    }

    @DeleteMapping("/{testId}")
    public void delete(@PathVariable Long testId) {
        adminGuard.requireAdmin();
        adminTestService.delete(testId);
    }
}
