package interview_prep.attempt;

import interview_prep.auth.CurrentUserContext;
import interview_prep.auth.UserAccount;
import interview_prep.content.MatchPairRepository;
import interview_prep.content.Question;
import interview_prep.content.QuestionOption;
import interview_prep.content.QuestionOptionRepository;
import interview_prep.content.QuestionType;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AiReviewService {
    private static final Logger log = LoggerFactory.getLogger(AiReviewService.class);

    private final TestAttemptRepository attempts;
    private final AttemptAnswerRepository answers;
    private final QuestionOptionRepository options;
    private final MatchPairRepository pairs;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public AiReviewService(TestAttemptRepository attempts,
                           AttemptAnswerRepository answers,
                           QuestionOptionRepository options,
                           MatchPairRepository pairs,
                           AiRestClientFactory restClientFactory,
                           @Value("${app.ai.openai.base-url}") String baseUrl,
                           @Value("${app.ai.openai.api-key:}") String apiKey,
                           @Value("${app.ai.openai.model:}") String model,
                           @Value("${app.ai.openai.connect-timeout:5s}") Duration connectTimeout,
                           @Value("${app.ai.openai.read-timeout:30s}") Duration readTimeout) {
        this.attempts = attempts;
        this.answers = answers;
        this.options = options;
        this.pairs = pairs;
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = restClientFactory.create(baseUrl, connectTimeout, readTimeout);
    }

    @Transactional(readOnly = true)
    public AttemptDtos.AiReviewResponse review(Long attemptId) {
        TestAttempt attempt = ownedCompletedAttempt(attemptId);
        List<AttemptAnswer> attemptAnswers = answers.findByAttemptIdOrderByQuestionPosition(attemptId);
        ReviewContext context = buildContext(attempt, attemptAnswers);

        if (apiKey == null || apiKey.isBlank() || model == null || model.isBlank()) {
            return fallbackReview(attemptId, context);
        }

        try {
            String generated = callModel(context);
            if (generated != null && !generated.isBlank()) {
                return new AttemptDtos.AiReviewResponse(
                        attemptId,
                        true,
                        generated,
                        topicReviews(context),
                        resources(context),
                        "Выбери одну слабую тему, прочитай ресурс из списка и затем пройди похожий тест повторно."
                );
            }
        } catch (RuntimeException exception) {
            log.warn("AI final review generation failed, fallback will be used: {}", exception.getMessage());
        }
        return fallbackReview(attemptId, context);
    }

    private TestAttempt ownedCompletedAttempt(Long attemptId) {
        UserAccount user = CurrentUserContext.getRequired();
        TestAttempt attempt = attempts.findById(attemptId)
                .orElseThrow(() -> new EntityNotFoundException("Attempt not found"));
        if (!Objects.equals(attempt.getUser().getId(), user.getId())) {
            throw new EntityNotFoundException("Attempt not found");
        }
        if (attempt.getStatus() != AttemptStatus.COMPLETED) {
            throw new IllegalArgumentException("Attempt is not completed yet");
        }
        return attempt;
    }

    private ReviewContext buildContext(TestAttempt attempt, List<AttemptAnswer> attemptAnswers) {
        List<AnswerReviewItem> incorrect = attemptAnswers.stream()
                .filter(answer -> !answer.isCorrect())
                .map(this::toReviewItem)
                .toList();

        List<AnswerReviewItem> sourceItems = incorrect.isEmpty()
                ? attemptAnswers.stream().map(this::toReviewItem).limit(4).toList()
                : incorrect;

        Map<String, List<AnswerReviewItem>> byTopic = sourceItems.stream()
                .collect(Collectors.groupingBy(
                        AnswerReviewItem::topic,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return new ReviewContext(
                attempt.getTest().getTitle(),
                attempt.getCorrectAnswers(),
                attempt.getTotalQuestions(),
                incorrect,
                byTopic
        );
    }

    private AnswerReviewItem toReviewItem(AttemptAnswer answer) {
        Question question = answer.getQuestion();
        return new AnswerReviewItem(
                question.getTopic(),
                question.getPrompt(),
                answer.getSubmittedAnswer(),
                correctAnswer(question)
        );
    }

    private String callModel(ReviewContext context) {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.2,
                "max_tokens", 1000,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", """
                                        You are an interview preparation mentor.
                                        Write concise practical feedback in Russian.
                                        Include 3-5 real public learning links as markdown links.
                                        """
                        ),
                        Map.of(
                                "role", "user",
                                "content", prompt(context)
                        )
                )
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(body)
                .retrieve()
                .body(Map.class);

        return extractMessageContent(response);
    }

    private String prompt(ReviewContext context) {
        StringBuilder builder = new StringBuilder();
        builder.append("Test: ").append(context.testTitle()).append('\n');
        builder.append("Score: ").append(context.correctAnswers()).append('/').append(context.totalQuestions()).append("\n\n");

        if (context.incorrectAnswers().isEmpty()) {
            builder.append("The student answered all questions correctly. Recommend next steps and useful resources.\n\n");
        } else {
            builder.append("Incorrect answers:\n");
            for (AnswerReviewItem item : context.incorrectAnswers()) {
                builder.append("- Topic: ").append(item.topic()).append('\n');
                builder.append("  Question: ").append(item.prompt()).append('\n');
                builder.append("  Submitted: ").append(item.submittedAnswer()).append('\n');
                builder.append("  Correct answer: ").append(item.correctAnswer()).append("\n\n");
            }
        }

        builder.append("Return sections:\n");
        builder.append("1. Краткий итог\n");
        builder.append("2. Темы, которые стоит подтянуть\n");
        builder.append("3. Ресурсы для изучения with markdown links\n");
        builder.append("4. Следующий шаг на 2-3 дня\n");
        return builder.toString();
    }

    private String extractMessageContent(Map<String, Object> response) {
        if (response == null) {
            return "";
        }
        Object choices = response.get("choices");
        if (!(choices instanceof List<?> choiceItems) || choiceItems.isEmpty()) {
            return "";
        }
        Object firstChoice = choiceItems.getFirst();
        if (!(firstChoice instanceof Map<?, ?> choiceMap)) {
            return "";
        }
        Object message = choiceMap.get("message");
        if (!(message instanceof Map<?, ?> messageMap)) {
            return "";
        }
        Object content = messageMap.get("content");
        return content instanceof String text ? text.trim() : "";
    }

    private AttemptDtos.AiReviewResponse fallbackReview(Long attemptId, ReviewContext context) {
        String summary = context.incorrectAnswers().isEmpty()
                ? "Отличный результат: все ответы верные. Можно переходить к более сложным вопросам и практическим задачам."
                : "Есть пробелы в темах: " + String.join(", ", context.byTopic().keySet()) + ". Ниже собраны рекомендации и ссылки для повторения.";

        return new AttemptDtos.AiReviewResponse(
                attemptId,
                false,
                summary,
                topicReviews(context),
                resources(context),
                context.incorrectAnswers().isEmpty()
                        ? "Реши 2-3 практические задачи по темам теста и попробуй объяснить ответы вслух."
                        : "Начни с первой слабой темы, прочитай материал по ссылке и затем повтори вопросы по этой теме."
        );
    }

    private List<AttemptDtos.AiTopicReview> topicReviews(ReviewContext context) {
        if (context.byTopic().isEmpty()) {
            return List.of(new AttemptDtos.AiTopicReview(
                    "Общий уровень",
                    "Ошибок по тесту нет.",
                    "Переходи к задачам сложнее и тренируй объяснение решений вслух."
            ));
        }

        return context.byTopic().entrySet().stream()
                .map(entry -> new AttemptDtos.AiTopicReview(
                        entry.getKey(),
                        "Ошибки или точки для повторения связаны с вопросами по этой теме.",
                        "Разбери корректный ответ и прочитай ресурс из блока resources."
                ))
                .toList();
    }

    private List<AttemptDtos.AiResource> resources(ReviewContext context) {
        List<AttemptDtos.AiResource> resources = new ArrayList<>();
        context.byTopic().keySet().forEach(topic -> resources.add(new AttemptDtos.AiResource(
                topic,
                "https://www.google.com/search?q=" + topic.replace(" ", "+") + "+technical+interview",
                "Материал поможет повторить тему " + topic
        )));
        return resources;
    }

    private String correctAnswer(Question question) {
        if (question.getType() == QuestionType.SINGLE_CHOICE || question.getType() == QuestionType.MULTIPLE_CHOICE) {
            return options.findByQuestionIdOrderById(question.getId()).stream()
                    .filter(QuestionOption::isCorrect)
                    .map(QuestionOption::getText)
                    .collect(Collectors.joining(", "));
        }
        if (question.getType() == QuestionType.MATCHING) {
            return pairs.findByQuestionIdOrderById(question.getId()).stream()
                    .map(pair -> pair.getLeftLabel() + " -> " + pair.getRightLabel())
                    .collect(Collectors.joining("; "));
        }
        return question.getCorrectTextAnswer() == null ? "" : question.getCorrectTextAnswer();
    }

    private record ReviewContext(
            String testTitle,
            int correctAnswers,
            int totalQuestions,
            List<AnswerReviewItem> incorrectAnswers,
            Map<String, List<AnswerReviewItem>> byTopic
    ) {
    }

    private record AnswerReviewItem(
            String topic,
            String prompt,
            String submittedAnswer,
            String correctAnswer
    ) {
    }
}
