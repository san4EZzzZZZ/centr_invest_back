package interview_prep.admin;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public List<AdminDtos.TestSummaryResponse> list(@RequestParam(required = false) String title,
                                                    @RequestParam(required = false) String language,
                                                    @RequestParam(required = false) String profession) {
        return adminTestService.list(adminGuard.requireAdmin(), title, language, profession);
    }

    @GetMapping("/{testId}")
    public AdminDtos.TestDetailsResponse get(@PathVariable Long testId) {
        return adminTestService.get(adminGuard.requireAdmin(), testId);
    }

    @PostMapping
    public AdminDtos.TestDetailsResponse create(@Valid @RequestBody AdminDtos.TestUpsertRequest request) {
        return adminTestService.create(adminGuard.requireAdmin(), request);
    }

    @PutMapping("/{testId}")
    public AdminDtos.TestDetailsResponse update(@PathVariable Long testId,
                                                @Valid @RequestBody AdminDtos.TestUpsertRequest request) {
        adminGuard.requireAdmin();
        return adminTestService.update(adminGuard.requireAdmin(), testId, request);
    }

    @DeleteMapping("/{testId}")
    public void delete(@PathVariable Long testId) {
        adminTestService.delete(adminGuard.requireAdmin(), testId);
    }
}
