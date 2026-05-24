package interview_prep.profile;

import interview_prep.attempt.AttemptDtos;
import interview_prep.attempt.AttemptService;
import interview_prep.auth.CurrentUser;
import interview_prep.auth.CurrentUserContext;
import interview_prep.auth.UserAccount;
import interview_prep.content.InterviewTest;
import interview_prep.content.InterviewTestRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final AttemptService attemptService;
    private final FavoriteTestRepository favorites;
    private final InterviewTestRepository tests;
    private final ProfileService profileService;
    private final AvatarStorageService avatars;

    public ProfileController(AttemptService attemptService, FavoriteTestRepository favorites, InterviewTestRepository tests,
                             ProfileService profileService, AvatarStorageService avatars) {
        this.attemptService = attemptService;
        this.favorites = favorites;
        this.tests = tests;
        this.profileService = profileService;
        this.avatars = avatars;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ProfileResponse profile() {
        UserAccount user = CurrentUserContext.getRequired();
        List<AttemptDtos.RecentAttemptResponse> recentAttempts = attemptService.recentCompleted(user.getId());
        return new ProfileResponse(CurrentUser.from(user), recentAttempts, favoriteResponses(user.getId()));
    }

    @GetMapping("/favorites")
    @Transactional(readOnly = true)
    public List<FavoriteTestResponse> favorites(@RequestParam(required = false) String title,
                                                @RequestParam(required = false) String language,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(defaultValue = "addedAtDesc") String sort) {
        UserAccount user = CurrentUserContext.getRequired();
        return favoriteResponses(user.getId(), title, language, page, size, sort);
    }

    @GetMapping("/completed-tests")
    @Transactional(readOnly = true)
    public List<AttemptDtos.CompletedTestResponse> completedTests() {
        UserAccount user = CurrentUserContext.getRequired();
        return attemptService.completedTests(user.getId());
    }

    @PatchMapping("/name")
    public CurrentUser updateUsername(@Valid @RequestBody ProfileRequests.UpdateUsernameRequest request) {
        return profileService.updateUsername(CurrentUserContext.getRequired(), request);
    }

    @PostMapping("/email/change/request")
    public interview_prep.auth.VerificationStartResponse requestEmailChange(
            @Valid @RequestBody ProfileRequests.RequestEmailChangeRequest request) {
        return profileService.requestEmailChange(CurrentUserContext.getRequired(), request);
    }

    @PostMapping("/email/change/confirm")
    public CurrentUser confirmEmailChange(@Valid @RequestBody ProfileRequests.ConfirmEmailChangeRequest request) {
        return profileService.confirmEmailChange(CurrentUserContext.getRequired(), request);
    }

    @PostMapping("/password/change/request")
    public interview_prep.auth.VerificationStartResponse requestPasswordChange() {
        return profileService.requestPasswordChange(CurrentUserContext.getRequired());
    }

    @PostMapping("/password/change/confirm")
    public void confirmPasswordChange(@Valid @RequestBody ProfileRequests.ConfirmPasswordChangeRequest request) {
        profileService.confirmPasswordChange(CurrentUserContext.getRequired(), request);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CurrentUser updateAvatar(@RequestParam("file") MultipartFile file) {
        return profileService.updateAvatar(CurrentUserContext.getRequired(), file);
    }

    @DeleteMapping("/avatar")
    public CurrentUser deleteAvatar() {
        return profileService.deleteAvatar(CurrentUserContext.getRequired());
    }

    @GetMapping("/avatar/{fileName}")
    public ResponseEntity<Resource> avatar(@PathVariable String fileName) {
        return ResponseEntity.ok()
                .contentType(mediaType(fileName))
                .body(avatars.load(fileName));
    }

    @PostMapping("/favorites/tests/{testId}")
    @Transactional
    public FavoriteTestResponse addFavorite(@PathVariable Long testId) {
        UserAccount user = CurrentUserContext.getRequired();
        InterviewTest test = tests.findById(testId)
                .orElseThrow(() -> new EntityNotFoundException("Test not found"));
        FavoriteTest favorite = favorites.findByUserIdAndTestId(user.getId(), testId)
                .orElseGet(() -> favorites.save(new FavoriteTest(user, test)));
        return toFavoriteResponse(favorite);
    }

    @DeleteMapping("/favorites/tests/{testId}")
    @Transactional
    public void removeFavorite(@PathVariable Long testId) {
        UserAccount user = CurrentUserContext.getRequired();
        favorites.findByUserIdAndTestId(user.getId(), testId).ifPresent(favorites::delete);
    }

    private List<FavoriteTestResponse> favoriteResponses(Long userId) {
        return favorites.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toFavoriteResponse)
                .toList();
    }

    private List<FavoriteTestResponse> favoriteResponses(Long userId, String title, String language, int page, int size,
                                                         String sort) {
        String titleFilter = blankToNull(title);
        String languageFilter = blankToNull(language);
        List<FavoriteTest> foundFavorites = titleFilter == null && languageFilter == null
                ? favorites.findByUserIdOrderByCreatedAtDesc(userId)
                : favorites.searchByUserId(userId, titleFilter, languageFilter);
        return foundFavorites.stream()
                .sorted(favoriteComparator(sort))
                .skip(offset(page, size))
                .limit(normalizedSize(size))
                .map(this::toFavoriteResponse)
                .toList();
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private FavoriteTestResponse toFavoriteResponse(FavoriteTest favorite) {
        InterviewTest test = favorite.getTest();
        return new FavoriteTestResponse(
                test.getId(),
                test.getProfession().getId(),
                test.getProfession().getTitle(),
                test.getTitle(),
                test.getShortDescription(),
                test.getDescription(),
                favorite.getCreatedAt()
        );
    }

    public record ProfileResponse(CurrentUser user, List<AttemptDtos.RecentAttemptResponse> recentAttempts,
                                  List<FavoriteTestResponse> favoriteTests) {
    }

    public record FavoriteTestResponse(Long testId, Long languageId, String languageTitle, String testTitle,
                                       String testShortDescription, String testDescription, Instant addedAt) {
    }

    private Comparator<FavoriteTest> favoriteComparator(String sort) {
        Comparator<FavoriteTest> byAddedAt = Comparator.comparing(FavoriteTest::getCreatedAt);
        Comparator<FavoriteTest> byTitle = Comparator.comparing(
                favorite -> favorite.getTest().getTitle(),
                String.CASE_INSENSITIVE_ORDER
        );
        Comparator<FavoriteTest> byLanguage = Comparator.comparing(
                favorite -> favorite.getTest().getProfession().getTitle(),
                String.CASE_INSENSITIVE_ORDER
        );
        return switch (sort == null ? "" : sort) {
            case "addedAtAsc" -> byAddedAt;
            case "titleAsc" -> byTitle.thenComparing(byAddedAt.reversed());
            case "titleDesc" -> byTitle.reversed().thenComparing(byAddedAt.reversed());
            case "languageAsc" -> byLanguage.thenComparing(byTitle);
            case "languageDesc" -> byLanguage.reversed().thenComparing(byTitle);
            default -> byAddedAt.reversed();
        };
    }

    private long offset(int page, int size) {
        return (long) Math.max(0, page) * normalizedSize(size);
    }

    private int normalizedSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

    private MediaType mediaType(String fileName) {
        if (fileName.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (fileName.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }
}
