package interview_prep.admin;

import interview_prep.attempt.AttemptAnswerRepository;
import interview_prep.attempt.AttemptQuestionRepository;
import interview_prep.attempt.TestAttempt;
import interview_prep.attempt.TestAttemptRepository;
import interview_prep.content.InterviewTest;
import interview_prep.content.InterviewTestRepository;
import interview_prep.content.MatchPair;
import interview_prep.content.MatchPairRepository;
import interview_prep.content.Profession;
import interview_prep.content.ProfessionRepository;
import interview_prep.content.Question;
import interview_prep.content.QuestionOption;
import interview_prep.content.QuestionOptionRepository;
import interview_prep.content.QuestionRepository;
import interview_prep.content.QuestionType;
import interview_prep.profile.FavoriteTestRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminTestService {
    private final ProfessionRepository professions;
    private final InterviewTestRepository tests;
    private final QuestionRepository questions;
    private final QuestionOptionRepository options;
    private final MatchPairRepository pairs;
    private final TestAttemptRepository attempts;
    private final AttemptAnswerRepository answers;
    private final AttemptQuestionRepository attemptQuestions;
    private final FavoriteTestRepository favorites;

    public AdminTestService(ProfessionRepository professions, InterviewTestRepository tests,
                            QuestionRepository questions, QuestionOptionRepository options,
                            MatchPairRepository pairs, TestAttemptRepository attempts,
                            AttemptAnswerRepository answers, AttemptQuestionRepository attemptQuestions,
                            FavoriteTestRepository favorites) {
        this.professions = professions;
        this.tests = tests;
        this.questions = questions;
        this.options = options;
        this.pairs = pairs;
        this.attempts = attempts;
        this.answers = answers;
        this.attemptQuestions = attemptQuestions;
        this.favorites = favorites;
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.TestSummaryResponse> list(String title, String profession) {
        String titleFilter = blankToNull(title);
        String professionFilter = blankToNull(profession);
        return tests.search(titleFilter, professionFilter).stream()
                .map(test -> new AdminDtos.TestSummaryResponse(
                        test.getId(),
                        test.getProfession().getId(),
                        test.getProfession().getTitle(),
                        test.getTitle(),
                        test.getShortDescription(),
                        test.getDescription(),
                        (int) questions.countByProfessionId(test.getProfession().getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminDtos.TestDetailsResponse get(Long testId) {
        InterviewTest test = findTest(testId);
        return toDetails(test);
    }

    @Transactional
    public AdminDtos.TestDetailsResponse create(AdminDtos.TestUpsertRequest request) {
        validate(request);
        Profession profession = professions.findById(request.professionId())
                .orElseThrow(() -> new EntityNotFoundException("Profession not found"));

        InterviewTest test = tests.save(new InterviewTest(
                profession,
                request.title().trim(),
                request.shortDescription().trim(),
                request.description().trim()
        ));
        saveQuestions(profession, request.questions());
        return toDetails(test);
    }

    @Transactional
    public AdminDtos.TestDetailsResponse update(Long testId, AdminDtos.TestUpsertRequest request) {
        validate(request);
        InterviewTest test = findTest(testId);
        Profession profession = professions.findById(request.professionId())
                .orElseThrow(() -> new EntityNotFoundException("Profession not found"));

        deleteAttempts(testId);
        deleteProfessionQuestions(profession.getId());

        test.setProfession(profession);
        test.setTitle(request.title().trim());
        test.setShortDescription(request.shortDescription().trim());
        test.setDescription(request.description().trim());
        saveQuestions(profession, request.questions());
        return toDetails(test);
    }

    @Transactional
    public void delete(Long testId) {
        InterviewTest test = findTest(testId);
        deleteAttempts(testId);
        favorites.deleteByTestId(testId);
        tests.delete(test);
    }

    private InterviewTest findTest(Long testId) {
        return tests.findById(testId)
                .orElseThrow(() -> new EntityNotFoundException("Test not found"));
    }

    private void saveQuestions(Profession profession, List<AdminDtos.QuestionUpsertRequest> requests) {
        for (int index = 0; index < requests.size(); index++) {
            AdminDtos.QuestionUpsertRequest request = requests.get(index);
            Question question = questions.save(new Question(
                    profession,
                    index + 1,
                    request.type(),
                    request.topic().trim(),
                    request.prompt().trim(),
                    trimToNull(request.correctTextAnswer()),
                    request.explanation().trim(),
                    request.readMoreUrl().trim()
            ));

            if (request.options() != null) {
                request.options().forEach(option -> options.save(new QuestionOption(
                        question,
                        option.text().trim(),
                        option.correct()
                )));
            }

            if (request.matchPairs() != null) {
                request.matchPairs().forEach(pair -> pairs.save(new MatchPair(
                        question,
                        pair.leftLabel().trim(),
                        pair.rightLabel().trim()
                )));
            }
        }
    }

    private void deleteAttempts(Long testId) {
        List<Long> attemptIds = attempts.findByTestId(testId).stream()
                .map(TestAttempt::getId)
                .toList();
        if (!attemptIds.isEmpty()) {
            answers.deleteByAttemptIdIn(attemptIds);
            attemptQuestions.deleteByAttemptIdIn(attemptIds);
        }
        attempts.deleteByTestId(testId);
    }

    private void deleteProfessionQuestions(Long professionId) {
        List<Long> affectedAttemptIds = attempts.findAll().stream()
                .filter(attempt -> attempt.getTest().getProfession().getId().equals(professionId))
                .map(TestAttempt::getId)
                .toList();
        if (!affectedAttemptIds.isEmpty()) {
            answers.deleteByAttemptIdIn(affectedAttemptIds);
            attemptQuestions.deleteByAttemptIdIn(affectedAttemptIds);
            affectedAttemptIds.forEach(attempts::deleteById);
        }

        questions.findByProfessionIdOrderByPosition(professionId).forEach(question -> {
            options.deleteByQuestionId(question.getId());
            pairs.deleteByQuestionId(question.getId());
            questions.delete(question);
        });
    }

    private AdminDtos.TestDetailsResponse toDetails(InterviewTest test) {
        return new AdminDtos.TestDetailsResponse(
                test.getId(),
                test.getProfession().getId(),
                test.getProfession().getTitle(),
                test.getTitle(),
                test.getShortDescription(),
                test.getDescription(),
                questions.findByProfessionIdOrderByPosition(test.getProfession().getId()).stream()
                        .map(this::toQuestionDetails)
                        .toList()
        );
    }

    private AdminDtos.QuestionDetailsResponse toQuestionDetails(Question question) {
        return new AdminDtos.QuestionDetailsResponse(
                question.getId(),
                question.getPosition(),
                question.getType(),
                question.getTopic(),
                question.getPrompt(),
                question.getCorrectTextAnswer(),
                question.getExplanation(),
                question.getReadMoreUrl(),
                options.findByQuestionIdOrderById(question.getId()).stream()
                        .map(option -> new AdminDtos.OptionDetailsResponse(
                                option.getId(),
                                option.getText(),
                                option.isCorrect()
                        ))
                        .toList(),
                pairs.findByQuestionIdOrderById(question.getId()).stream()
                        .map(pair -> new AdminDtos.MatchPairDetailsResponse(
                                pair.getId(),
                                pair.getLeftLabel(),
                                pair.getRightLabel()
                        ))
                        .toList()
        );
    }

    private void validate(AdminDtos.TestUpsertRequest request) {
        if (request.questions() == null || request.questions().isEmpty()) {
            throw new IllegalArgumentException("Test must contain at least one question");
        }

        request.questions().forEach(this::validateQuestion);
    }

    private void validateQuestion(AdminDtos.QuestionUpsertRequest question) {
        List<AdminDtos.OptionUpsertRequest> questionOptions = question.options() == null ? List.of() : question.options();
        List<AdminDtos.MatchPairUpsertRequest> questionPairs = question.matchPairs() == null ? List.of() : question.matchPairs();

        if (question.type() == QuestionType.SINGLE_CHOICE) {
            long correctCount = questionOptions.stream().filter(AdminDtos.OptionUpsertRequest::correct).count();
            if (questionOptions.size() < 2 || correctCount != 1) {
                throw new IllegalArgumentException("SINGLE_CHOICE question must contain at least 2 options and exactly 1 correct option");
            }
        }

        if (question.type() == QuestionType.MULTIPLE_CHOICE) {
            long correctCount = questionOptions.stream().filter(AdminDtos.OptionUpsertRequest::correct).count();
            if (questionOptions.size() < 2 || correctCount < 1) {
                throw new IllegalArgumentException("MULTIPLE_CHOICE question must contain at least 2 options and at least 1 correct option");
            }
        }

        if (question.type() == QuestionType.MATCHING && questionPairs.isEmpty()) {
            throw new IllegalArgumentException("MATCHING question must contain matchPairs");
        }

        if (question.type() == QuestionType.SHORT_TEXT && trimToNull(question.correctTextAnswer()) == null) {
            throw new IllegalArgumentException("SHORT_TEXT question must contain correctTextAnswer");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return trimToNull(value);
    }
}
