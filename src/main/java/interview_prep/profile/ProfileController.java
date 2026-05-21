package interview_prep.profile;

import interview_prep.attempt.AttemptDtos;
import interview_prep.attempt.AttemptService;
import interview_prep.auth.CurrentUser;
import interview_prep.auth.CurrentUserContext;
import interview_prep.auth.UserAccount;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final AttemptService attemptService;

    public ProfileController(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @GetMapping
    public ProfileResponse profile() {
        UserAccount user = CurrentUserContext.getRequired();
        List<AttemptDtos.RecentAttemptResponse> recentAttempts = attemptService.recentCompleted(user.getId());
        return new ProfileResponse(CurrentUser.from(user), recentAttempts);
    }

    public record ProfileResponse(CurrentUser user, List<AttemptDtos.RecentAttemptResponse> recentAttempts) {
    }
}
