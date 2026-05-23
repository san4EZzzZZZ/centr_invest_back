package interview_prep.content;

import jakarta.persistence.EntityNotFoundException;
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

    @GetMapping("/professions")
    public List<ContentDtos.ProfessionResponse> professions(@RequestParam(required = false) String title,
                                                            @RequestParam(required = false) String profession) {
        if (hasText(title) || hasText(profession)) {
            return searchedProfessions(blankToNull(title), blankToNull(profession));
        }

        return professions.findAll().stream()
                .map(foundProfession -> new ContentDtos.ProfessionResponse(
                        foundProfession.getId(),
                        foundProfession.getTitle(),
                        foundProfession.getDescription(),
                        tests.findByProfessionIdOrderByTitle(foundProfession.getId()).stream()
                                .map(test -> new ContentDtos.TestSummary(
                                        test.getId(),
                                        test.getTitle(),
                                        test.getShortDescription(),
                                        test.getDescription(),
                                        (int) questions.countByProfessionId(foundProfession.getId())
                                ))
                                .toList()
                ))
                .toList();
    }

    @GetMapping("/tests/{testId}")
    public ContentDtos.TestResponse test(@PathVariable Long testId) {
        InterviewTest test = tests.findById(testId)
                .orElseThrow(() -> new EntityNotFoundException("Test not found"));
        return new ContentDtos.TestResponse(
                test.getId(),
                test.getProfession().getId(),
                test.getTitle(),
                test.getShortDescription(),
                test.getDescription(),
                questions.findByProfessionIdOrderByPosition(test.getProfession().getId()).stream()
                        .map(mapper::toQuestionResponse)
                        .toList()
        );
    }

    private List<ContentDtos.ProfessionResponse> searchedProfessions(String title, String profession) {
        return tests.search(title, profession).stream()
                .collect(Collectors.groupingBy(InterviewTest::getProfession, LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> new ContentDtos.ProfessionResponse(
                        entry.getKey().getId(),
                        entry.getKey().getTitle(),
                        entry.getKey().getDescription(),
                        entry.getValue().stream()
                                .map(test -> new ContentDtos.TestSummary(
                                        test.getId(),
                                        test.getTitle(),
                                        test.getShortDescription(),
                                        test.getDescription(),
                                        (int) questions.countByProfessionId(test.getProfession().getId())
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
}
