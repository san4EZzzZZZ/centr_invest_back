package interview_prep.content;

import interview_prep.auth.CurrentUserContext;
import interview_prep.auth.UserAccount;
import interview_prep.profile.FavoriteTestRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ContentController {
    private final ProfessionRepository professions;
    private final InterviewTestRepository tests;
    private final QuestionRepository questions;
    private final ContentMapper mapper;
    private final FavoriteTestRepository favorites;

    public ContentController(ProfessionRepository professions, InterviewTestRepository tests,
                             QuestionRepository questions, ContentMapper mapper, FavoriteTestRepository favorites) {
        this.professions = professions;
        this.tests = tests;
        this.questions = questions;
        this.mapper = mapper;
        this.favorites = favorites;
    }

    @GetMapping({"/languages", "/professions"})
    @Transactional(readOnly = true)
    public List<ContentDtos.LanguageResponse> languages(@RequestParam(required = false) String title,
                                                        @RequestParam(required = false) String language,
                                                        @RequestParam(required = false) String profession,
                                                        @RequestParam(defaultValue = "titleAsc") String sort) {
        String languageFilter = firstPresent(language, profession);
        if (hasText(title) || hasText(languageFilter)) {
            return searchedLanguages(blankToNull(title), blankToNull(languageFilter), sort);
        }

        return DemoDataInitializer.LANGUAGE_TITLES.stream()
                .map(professions::findByTitle)
                .flatMap(java.util.Optional::stream)
                .map(foundProfession -> new ContentDtos.LanguageResponse(
                        foundProfession.getId(),
                        foundProfession.getTitle(),
                        foundProfession.getDescription(),
                        tests.findByProfessionIdOrderByTitle(foundProfession.getId()).stream()
                                .sorted(testComparator(sort))
                                .map(this::toTestSummary)
                                .toList()
                ))
                .toList();
    }

    @GetMapping("/tests/{testId}")
    @Transactional(readOnly = true)
    public ContentDtos.TestResponse test(@PathVariable Long testId) {
        InterviewTest test = tests.findById(testId)
                .orElseThrow(() -> new EntityNotFoundException("Test not found"));
        return new ContentDtos.TestResponse(
                test.getId(),
                test.getProfession().getId(),
                test.getTitle(),
                test.getShortDescription(),
                test.getDescription(),
                questionsForTest(test).stream()
                        .map(mapper::toQuestionResponse)
                        .toList()
        );
    }

    @GetMapping({"/languages/{languageId}/tests", "/professions/{languageId}/tests"})
    @Transactional(readOnly = true)
    public List<ContentDtos.TestSummary> testsByLanguage(@PathVariable Long languageId,
                                                         @RequestParam(required = false) String title,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size,
                                                         @RequestParam(defaultValue = "titleAsc") String sort) {
        Profession language = professions.findById(languageId)
                .orElseThrow(() -> new EntityNotFoundException("Language not found"));
        if (!DemoDataInitializer.LANGUAGE_TITLES.contains(language.getTitle())) {
            throw new EntityNotFoundException("Language not found");
        }

        List<InterviewTest> foundTests = hasText(title)
                ? tests.searchByLanguageIdAndTitle(languageId, title.trim())
                : tests.findByLanguageIdWithLanguage(languageId);

        return foundTests.stream()
                .sorted(testComparator(sort))
                .skip(offset(page, size))
                .limit(normalizedSize(size))
                .map(this::toTestSummary)
                .toList();
    }

    private List<ContentDtos.LanguageResponse> searchedLanguages(String title, String language, String sort) {
        return findTests(title, language).stream()
                .filter(test -> DemoDataInitializer.LANGUAGE_TITLES.contains(test.getProfession().getTitle()))
                .collect(Collectors.groupingBy(InterviewTest::getProfession, LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> new ContentDtos.LanguageResponse(
                        entry.getKey().getId(),
                        entry.getKey().getTitle(),
                        entry.getKey().getDescription(),
                        entry.getValue().stream()
                                .sorted(testComparator(sort))
                                .map(this::toTestSummary)
                                .toList()
                ))
                .toList();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String firstPresent(String preferred, String fallback) {
        return hasText(preferred) ? preferred : fallback;
    }

    private List<InterviewTest> findTests(String title, String language) {
        if (title != null && language != null) {
            return tests.searchByTitleAndLanguage(title, language);
        }
        if (title != null) {
            return tests.searchByTitle(title);
        }
        if (language != null) {
            return tests.searchByLanguage(language);
        }
        return tests.findAllWithLanguage();
    }

    private int questionCount(InterviewTest test) {
        return (int) questions.countByTestId(test.getId());
    }

    private ContentDtos.TestSummary toTestSummary(InterviewTest test) {
        return new ContentDtos.TestSummary(
                test.getId(),
                test.getTitle(),
                test.getShortDescription(),
                test.getDescription(),
                questionCount(test),
                isFavorite(test.getId())
        );
    }

    private boolean isFavorite(Long testId) {
        UserAccount user = CurrentUserContext.get();
        return user != null && favorites.existsByUserIdAndTestId(user.getId(), testId);
    }

    private Comparator<InterviewTest> testComparator(String sort) {
        Comparator<InterviewTest> byTitle = Comparator.comparing(InterviewTest::getTitle, String.CASE_INSENSITIVE_ORDER);
        Comparator<InterviewTest> byQuestionCount = Comparator.comparingInt(this::questionCount);
        return switch (sort == null ? "" : sort) {
            case "titleDesc" -> byTitle.reversed();
            case "questionCountAsc" -> byQuestionCount.thenComparing(byTitle);
            case "questionCountDesc" -> byQuestionCount.reversed().thenComparing(byTitle);
            default -> byTitle;
        };
    }

    private long offset(int page, int size) {
        return (long) Math.max(0, page) * normalizedSize(size);
    }

    private int normalizedSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

    private List<Question> questionsForTest(InterviewTest test) {
        return questions.findByTestIdOrderByPosition(test.getId());
    }
}
