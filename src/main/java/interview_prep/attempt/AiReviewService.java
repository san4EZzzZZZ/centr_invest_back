package interview_prep.attempt;

import interview_prep.auth.CurrentUserContext;
import interview_prep.auth.UserAccount;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AiReviewService {
    private final TestAttemptRepository attempts;
    private final AttemptAnswerRepository answers;
    private final RestClient restClient;
    private final String openAiApiKey;
    private final String model;

    public AiReviewService(TestAttemptRepository attempts,
                           AttemptAnswerRepository answers,
                           @Value("${app.ai.openai.api-key:}") String openAiApiKey,
                           @Value("${app.ai.openai.model:gpt-4.1-mini}") String model) {
        this.attempts = attempts;
        this.answers = answers;
        this.openAiApiKey = openAiApiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .build();
    }

    @Transactional(readOnly = true)
    public AttemptDtos.AiReviewResponse review(Long attemptId) {
        TestAttempt attempt = ownedCompletedAttempt(attemptId);
        List<AttemptAnswer> attemptAnswers = answers.findByAttemptIdOrderByQuestionPosition(attemptId);
        ReviewContext context = buildContext(attempt, attemptAnswers);

        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return fallbackReview(attemptId, context);
        }

        try {
            String generated = callOpenAi(context);
            return fromGeneratedText(attemptId, context, generated);
        } catch (RuntimeException exception) {
            return fallbackReview(attemptId, context);
        }
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
                .map(answer -> new AnswerReviewItem(
                        answer.getQuestion().getTopic(),
                        answer.getQuestion().getPrompt(),
                        answer.getSubmittedAnswer(),
                        answer.getQuestion().getExplanation(),
                        answer.getQuestion().getReadMoreUrl()
                ))
                .toList();

        List<AnswerReviewItem> sourceItems = incorrect.isEmpty()
                ? attemptAnswers.stream()
                .map(answer -> new AnswerReviewItem(
                        answer.getQuestion().getTopic(),
                        answer.getQuestion().getPrompt(),
                        answer.getSubmittedAnswer(),
                        answer.getQuestion().getExplanation(),
                        answer.getQuestion().getReadMoreUrl()
                ))
                .limit(4)
                .toList()
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

    private String callOpenAi(ReviewContext context) {
        Map<String, Object> body = Map.of(
                "model", model,
                "input", prompt(context),
                "max_output_tokens", 900
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + openAiApiKey)
                .body(body)
                .retrieve()
                .body(Map.class);

        return extractOutputText(response);
    }

    private String prompt(ReviewContext context) {
        StringBuilder builder = new StringBuilder();
        builder.append("You are an interview preparation mentor. ");
        builder.append("Write a final review in Russian for a student after a technical interview-prep test. ");
        builder.append("Do not invent URLs. Use only URLs from the provided context. ");
        builder.append("Keep the response concise and practical.\n\n");
        builder.append("Test: ").append(context.testTitle()).append('\n');
        builder.append("Score: ").append(context.correctAnswers()).append('/').append(context.totalQuestions()).append("\n\n");

        if (context.incorrectAnswers().isEmpty()) {
            builder.append("The student answered all questions correctly. Recommend next steps and still include useful resources from context.\n\n");
        } else {
            builder.append("Incorrect answers:\n");
            for (AnswerReviewItem item : context.incorrectAnswers()) {
                builder.append("- Topic: ").append(item.topic()).append('\n');
                builder.append("  Question: ").append(item.prompt()).append('\n');
                builder.append("  Submitted: ").append(item.submittedAnswer()).append('\n');
                builder.append("  Explanation: ").append(item.explanation()).append('\n');
                builder.append("  Resource: ").append(item.readMoreUrl()).append("\n\n");
            }
        }

        builder.append("Return sections:\n");
        builder.append("1. Краткий итог\n");
        builder.append("2. Темы, которые стоит подтянуть\n");
        builder.append("3. Ресурсы для изучения with markdown links\n");
        builder.append("4. Следующий шаг на 2-3 дня\n");
        return builder.toString();
    }

    private String extractOutputText(Map<String, Object> response) {
        if (response == null) {
            return "";
        }
        Object directText = response.get("output_text");
        if (directText instanceof String text && !text.isBlank()) {
            return text;
        }

        Object output = response.get("output");
        if (!(output instanceof List<?> outputItems)) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (Object outputItem : outputItems) {
            if (!(outputItem instanceof Map<?, ?> outputMap)) {
                continue;
            }
            Object content = outputMap.get("content");
            if (!(content instanceof List<?> contentItems)) {
                continue;
            }
            for (Object contentItem : contentItems) {
                if (contentItem instanceof Map<?, ?> contentMap) {
                    Object text = contentMap.get("text");
                    if (text instanceof String value) {
                        builder.append(value).append('\n');
                    }
                }
            }
        }
        return builder.toString().trim();
    }

    private AttemptDtos.AiReviewResponse fromGeneratedText(Long attemptId, ReviewContext context, String generated) {
        if (generated == null || generated.isBlank()) {
            return fallbackReview(attemptId, context);
        }

        return new AttemptDtos.AiReviewResponse(
                attemptId,
                true,
                generated,
                topicReviews(context),
                resources(context),
                "Выбери одну слабую тему, прочитай ресурс из списка и затем пройди похожий тест повторно."
        );
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
                        "Разбери пояснение к вопросу и прочитай ресурс из блока resources."
                ))
                .toList();
    }

    private List<AttemptDtos.AiResource> resources(ReviewContext context) {
        List<AttemptDtos.AiResource> resources = new ArrayList<>();
        context.byTopic().forEach((topic, items) -> items.stream()
                .map(AnswerReviewItem::readMoreUrl)
                .filter(url -> url != null && !url.isBlank())
                .distinct()
                .forEach(url -> resources.add(new AttemptDtos.AiResource(
                        topic,
                        url,
                        "Материал поможет повторить тему " + topic
                ))));
        return resources;
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
            String explanation,
            String readMoreUrl
    ) {
    }
}
