package interview_prep.content;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ContentController {
    private final ProfessionRepository professions;
    private final InterviewTestRepository tests;
    private final QuestionRepository questions;
    private final ContentMapper mapper;

    public ContentController(ProfessionRepository professions, InterviewTestRepository tests,
                             QuestionRepository questions, ContentMapper mapper) {
        this.professions = professions;
        this.tests = tests;
        this.questions = questions;
        this.mapper = mapper;
    }

    @GetMapping({"/languages", "/professions"})
    @Transactional(readOnly = true)
    public List<ContentDtos.LanguageResponse> languages(@RequestParam(required = false) String title,
                                                        @RequestParam(required = false) String language,
                                                        @RequestParam(required = false) String profession) {
        String languageFilter = firstPresent(language, profession);
        if (hasText(title) || hasText(languageFilter)) {
            return searchedLanguages(blankToNull(title), blankToNull(languageFilter));
        }

        return DemoDataInitializer.LANGUAGE_TITLES.stream()
                .map(professions::findByTitle)
                .flatMap(java.util.Optional::stream)
                .map(foundProfession -> new ContentDtos.LanguageResponse(
                        foundProfession.getId(),
                        foundProfession.getTitle(),
                        foundProfession.getDescription(),
                        tests.findByProfessionIdOrderByTitle(foundProfession.getId()).stream()
                                .map(test -> new ContentDtos.TestSummary(
                                        test.getId(),
                                        test.getTitle(),
                                        test.getShortDescription(),
                                        test.getDescription(),
                                        questionCount(test)
                                ))
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

    private List<ContentDtos.LanguageResponse> searchedLanguages(String title, String language) {
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
                                .map(test -> new ContentDtos.TestSummary(
                                        test.getId(),
                                        test.getTitle(),
                                        test.getShortDescription(),
                                        test.getDescription(),
                                        questionCount(test)
                                ))
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

    private List<Question> questionsForTest(InterviewTest test) {
        return questions.findByTestIdOrderByPosition(test.getId());
    }
}
