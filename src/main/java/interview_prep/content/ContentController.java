package interview_prep.content;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public List<ContentDtos.ProfessionResponse> professions() {
        return professions.findAll().stream()
                .map(profession -> new ContentDtos.ProfessionResponse(
                        profession.getId(),
                        profession.getTitle(),
                        profession.getDescription(),
                        tests.findByProfessionIdOrderByTitle(profession.getId()).stream()
                                .map(test -> new ContentDtos.TestSummary(
                                        test.getId(),
                                        test.getTitle(),
                                        test.getDescription(),
                                        (int) questions.countByTestId(test.getId())
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
                test.getDescription(),
                questions.findByTestIdOrderByPosition(testId).stream()
                        .map(mapper::toQuestionResponse)
                        .toList()
        );
    }
}
