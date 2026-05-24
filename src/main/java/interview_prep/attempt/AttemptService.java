package interview_prep.attempt;

import interview_prep.auth.CurrentUserContext;
import interview_prep.auth.UserAccount;
import interview_prep.content.ContentMapper;
import interview_prep.content.InterviewTest;
import interview_prep.content.InterviewTestRepository;
import interview_prep.content.MatchPairRepository;
import interview_prep.content.Question;
import interview_prep.content.QuestionOption;
import interview_prep.content.QuestionOptionRepository;
import interview_prep.content.QuestionRepository;
import interview_prep.content.QuestionType;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AttemptService {
    private final InterviewTestRepository tests;
    private final QuestionRepository questions;
    private final QuestionOptionRepository options;
    private final MatchPairRepository pairs;
    private final TestAttemptRepository attempts;
    private final AttemptAnswerRepository answers;
    private final AttemptQuestionRepository attemptQuestions;
    private final ContentMapper mapper;
    private final ShortTextAnswerReviewService shortTextAnswerReviewService;
    private final AiExplanationService aiExplanationService;
    private final AiReviewService aiReviewService;

    public AttemptService(InterviewTestRepository tests, QuestionRepository questions, QuestionOptionRepository options,
                          MatchPairRepository pairs, TestAttemptRepository attempts, AttemptAnswerRepository answers,
                          AttemptQuestionRepository attemptQuestions, ContentMapper mapper,
                          ShortTextAnswerReviewService shortTextAnswerReviewService,
                          AiExplanationService aiExplanationService,
                          AiReviewService aiReviewService) {
        this.tests = tests;
        this.questions = questions;
        this.options = options;
        this.pairs = pairs;
        this.attempts = attempts;
        this.answers = answers;
        this.attemptQuestions = attemptQuestions;
        this.mapper = mapper;
        this.shortTextAnswerReviewService = shortTextAnswerReviewService;
        this.aiExplanationService = aiExplanationService;
        this.aiReviewService = aiReviewService;
    }

    @Transactional
    public AttemptDtos.StartAttemptResponse start(Long testId) {
        UserAccount user = CurrentUserContext.getRequired();
        InterviewTest test = tests.findById(testId)
                .orElseThrow(() -> new EntityNotFoundException("Test not found"));
        List<Question> selectedQuestions = composeQuestions(test);
        if (selectedQuestions.isEmpty()) {
            throw new IllegalArgumentException("Question pool is empty for this test");
        }

        TestAttempt attempt = attempts.save(new TestAttempt(user, test, selectedQuestions.size()));
        for (int index = 0; index < selectedQuestions.size(); index++) {
            attemptQuestions.save(new AttemptQuestion(attempt, selectedQuestions.get(index), index + 1));
        }
        Question first = questionAt(attempt.getId(), 1);
        return new AttemptDtos.StartAttemptResponse(attempt.getId(), mapper.toQuestionResponse(first));
    }

    @Transactional(readOnly = true)
    public AttemptDtos.AttemptStateResponse state(Long attemptId) {
        TestAttempt attempt = ownedAttempt(attemptId);
        Question question = attempt.getStatus() == AttemptStatus.COMPLETED
                ? null
                : questionAt(attempt.getId(), attempt.getCurrentPosition());
        return new AttemptDtos.AttemptStateResponse(
                attempt.getId(),
                attempt.getStatus(),
                attempt.getCurrentPosition(),
                attempt.getTotalQuestions(),
                question == null ? null : mapper.toQuestionResponse(question)
        );
    }

    @Transactional
    public AttemptDtos.AnswerResponse answer(Long attemptId, AnswerRequest request) {
        TestAttempt attempt = ownedAttempt(attemptId);
        if (attempt.getStatus() == AttemptStatus.COMPLETED) {
            throw new IllegalArgumentException("Attempt is already completed");
        }

        Question question = questionAt(attempt.getId(), attempt.getCurrentPosition());
        if (answers.existsByAttemptIdAndQuestionId(attemptId, question.getId())) {
            throw new IllegalArgumentException("Question is already answered");
        }

        AnswerCheck answerCheck = checkAnswer(question, request);
        boolean correct = answerCheck.correct();
        AiExplanationService.GeneratedExplanation generatedExplanation =
                aiExplanationService.explain(question, request, correct);
        answers.save(new AttemptAnswer(
                attempt,
                question,
                correct,
                submittedAnswer(request),
                answerCheck.checkedByAi(),
                answerCheck.aiConfidence(),
                answerCheck.aiReason()
        ));
        if (correct) {
            attempt.setCorrectAnswers(attempt.getCorrectAnswers() + 1);
        }

        AttemptDtos.ResultResponse result = null;
        Question nextQuestion = null;
        if (attempt.getCurrentPosition() >= attempt.getTotalQuestions()) {
            attempt.setStatus(AttemptStatus.COMPLETED);
            attempt.setCompletedAt(Instant.now());
            result = result(attempt);
        } else {
            attempt.setCurrentPosition(attempt.getCurrentPosition() + 1);
            nextQuestion = questionAt(attempt.getId(), attempt.getCurrentPosition());
        }

        return new AttemptDtos.AnswerResponse(
                correct,
                generatedExplanation.explanation(),
                generatedExplanation.readMoreUrl(),
                generatedExplanation.generatedByAi(),
                answerCheck.checkedByAi(),
                answerCheck.aiConfidence(),
                nextQuestion == null ? null : mapper.toQuestionResponse(nextQuestion),
                result
        );
    }

    @Transactional(readOnly = true)
    public AttemptDtos.ResultResponse result(Long attemptId) {
        TestAttempt attempt = ownedAttempt(attemptId);
        if (attempt.getStatus() != AttemptStatus.COMPLETED) {
            throw new IllegalArgumentException("Attempt is not completed yet");
        }
        return result(attempt);
    }

    @Transactional(readOnly = true)
    public List<AttemptDtos.RecentAttemptResponse> recentCompleted(Long userId) {
        return attempts.findTop5ByUserIdAndStatusOrderByStartedAtDesc(userId, AttemptStatus.COMPLETED).stream()
                .map(attempt -> new AttemptDtos.RecentAttemptResponse(
                        attempt.getId(),
                        attempt.getTest().getProfession().getTitle(),
                        attempt.getTest().getTitle(),
                        attempt.getCorrectAnswers(),
                        attempt.getTotalQuestions(),
                        attempt.getCompletedAt()
                ))
                .toList();
    }

    private TestAttempt ownedAttempt(Long attemptId) {
        UserAccount user = CurrentUserContext.getRequired();
        TestAttempt attempt = attempts.findById(attemptId)
                .orElseThrow(() -> new EntityNotFoundException("Attempt not found"));
        if (!Objects.equals(attempt.getUser().getId(), user.getId())) {
            throw new EntityNotFoundException("Attempt not found");
        }
        return attempt;
    }

    private Question questionAt(Long attemptId, int position) {
        return attemptQuestions.findByAttemptIdAndPosition(attemptId, position)
                .map(AttemptQuestion::getQuestion)
                .orElseThrow(() -> new EntityNotFoundException("Question not found"));
    }

    private List<Question> composeQuestions(InterviewTest test) {
        List<Question> selected = new ArrayList<>();
        selected.addAll(takeRandom(test, QuestionType.SINGLE_CHOICE, 2));
        selected.addAll(takeRandom(test, QuestionType.MULTIPLE_CHOICE, 2));
        selected.addAll(takeRandom(test, QuestionType.MATCHING, 1));
        selected.addAll(takeRandom(test, QuestionType.SHORT_TEXT, 2));
        Collections.shuffle(selected);
        return selected;
    }

    private List<Question> takeRandom(InterviewTest test, QuestionType type, int limit) {
        List<Question> pool = new ArrayList<>(questions.findByTestIdAndTypeOrderByPosition(test.getId(), type));
        Collections.shuffle(pool);
        return pool.stream().limit(limit).toList();
    }

    private AnswerCheck checkAnswer(Question question, AnswerRequest request) {
        if (question.getType() == QuestionType.SINGLE_CHOICE || question.getType() == QuestionType.MULTIPLE_CHOICE) {
            Set<Long> expected = options.findByQuestionIdOrderById(question.getId()).stream()
                    .filter(QuestionOption::isCorrect)
                    .map(QuestionOption::getId)
                    .collect(Collectors.toSet());
            Set<Long> actual = request.selectedOptionIds() == null
                    ? Set.of()
                    : new HashSet<>(request.selectedOptionIds());
            return AnswerCheck.strict(expected.equals(actual));
        }
        if (question.getType() == QuestionType.MATCHING) {
            Map<String, String> expected = pairs.findByQuestionIdOrderById(question.getId()).stream()
                    .collect(Collectors.toMap(pair -> pair.getLeftLabel(), pair -> pair.getRightLabel()));
            return AnswerCheck.strict(expected.equals(request.matches() == null ? Map.of() : request.matches()));
        }
        ShortTextAnswerReviewService.Evaluation evaluation =
                shortTextAnswerReviewService.evaluate(question, request.textAnswer());
        return new AnswerCheck(
                evaluation.correct(),
                evaluation.checkedByAi(),
                evaluation.confidence(),
                evaluation.reason()
        );
    }

    private AttemptDtos.ResultResponse result(TestAttempt attempt) {
        List<String> weakTopics = answers.findByAttemptIdOrderByQuestionPosition(attempt.getId()).stream()
                .filter(answer -> !answer.isCorrect())
                .map(answer -> answer.getQuestion().getTopic())
                .distinct()
                .toList();

        String recommendation = weakTopics.isEmpty()
                ? "Отличный результат. Можно переходить к задачам уровня junior+ и практическим проектам."
                : "Повтори темы: " + String.join(", ", weakTopics) + ". После этого пройди тест еще раз и сравни результат.";

        return new AttemptDtos.ResultResponse(
                attempt.getId(),
                attempt.getTest().getTitle(),
                attempt.getCorrectAnswers(),
                attempt.getTotalQuestions(),
                weakTopics,
                recommendation,
                aiReviewService.review(attempt.getId()),
                attempt.getCompletedAt()
        );
    }

    private String submittedAnswer(AnswerRequest request) {
        return "selectedOptionIds=%s; matches=%s; textAnswer=%s".formatted(
                request.selectedOptionIds(),
                request.matches(),
                request.textAnswer()
        );
    }

    private record AnswerCheck(boolean correct, boolean checkedByAi, Double aiConfidence, String aiReason) {
        private static AnswerCheck strict(boolean correct) {
            return new AnswerCheck(correct, false, null, null);
        }
    }
}
