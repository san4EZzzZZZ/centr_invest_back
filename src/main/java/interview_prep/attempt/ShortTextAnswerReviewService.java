package interview_prep.attempt;

import interview_prep.content.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.text.Normalizer;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class ShortTextAnswerReviewService {
    private static final Logger log = LoggerFactory.getLogger(ShortTextAnswerReviewService.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final boolean enabled;
    private final double minConfidence;

    public ShortTextAnswerReviewService(AiRestClientFactory restClientFactory,
                                        @Value("${app.ai.openai.base-url}") String baseUrl,
                                        @Value("${app.ai.openai.api-key:}") String apiKey,
                                        @Value("${app.ai.openai.model:}") String model,
                                        @Value("${app.ai.answer-check.enabled:true}") boolean enabled,
                                        @Value("${app.ai.answer-check.min-confidence:0.75}") double minConfidence,
                                        @Value("${app.ai.openai.connect-timeout:5s}") Duration connectTimeout,
                                        @Value("${app.ai.openai.read-timeout:30s}") Duration readTimeout) {
        this.restClient = restClientFactory.create(baseUrl, connectTimeout, readTimeout);
        this.apiKey = apiKey;
        this.model = model;
        this.enabled = enabled;
        this.minConfidence = minConfidence;
    }

    public Evaluation evaluate(Question question, String userAnswer) {
        if (normalize(question.getCorrectTextAnswer()).equals(normalize(userAnswer))) {
            return new Evaluation(true, false, 1.0, "Exact normalized match");
        }

        if (!enabled || apiKey == null || apiKey.isBlank() || model == null || model.isBlank()) {
            return new Evaluation(false, false, 0.0, "AI answer check is not configured");
        }

        try {
            AiDecision decision = callModel(question, userAnswer);
            boolean accepted = decision.correct() && decision.confidence() >= minConfidence;
            return new Evaluation(accepted, true, decision.confidence(), decision.reason());
        } catch (RuntimeException exception) {
            log.warn("AI short text answer check failed, strict check will be used: {}", exception.getMessage());
            return new Evaluation(false, false, 0.0, "AI answer check failed, strict check was used");
        }
    }

    private AiDecision callModel(Question question, String userAnswer) {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0,
                "max_tokens", 120,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", systemPrompt()
                        ),
                        Map.of(
                                "role", "user",
                                "content", userPrompt(question, userAnswer)
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

        return parseDecision(extractMessageContent(response));
    }

    private String systemPrompt() {
        return """
                You grade short text answers for a technical interview preparation app.
                Compare the student's answer with the expected answer and the question context.
                Accept small typos, inflection differences, transliteration, or another language if the meaning is clearly equivalent.
                Reject vague, incomplete, contradictory, or overly broad answers.
                Return exactly one line in this format:
                CORRECT|confidence from 0 to 1|short reason
                or
                INCORRECT|confidence from 0 to 1|short reason
                """;
    }

    private String userPrompt(Question question, String userAnswer) {
        return """
                Topic: %s
                Question: %s
                Expected answer: %s
                Student answer: %s
                """.formatted(
                safe(question.getTopic()),
                safe(question.getPrompt()),
                safe(question.getCorrectTextAnswer()),
                safe(userAnswer)
        );
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

    private AiDecision parseDecision(String content) {
        String[] parts = content == null ? new String[0] : content.trim().split("\\|", 3);
        if (parts.length < 2) {
            return new AiDecision(false, 0.0, "Model returned an invalid decision");
        }

        boolean correct = "CORRECT".equalsIgnoreCase(parts[0].trim());
        double confidence = parseConfidence(parts[1]);
        String reason = parts.length == 3 && !parts[2].isBlank()
                ? parts[2].trim()
                : "No reason provided";
        return new AiDecision(correct, confidence, reason);
    }

    private double parseConfidence(String value) {
        try {
            double parsed = Double.parseDouble(value.trim().replace(',', '.'));
            return Math.max(0.0, Math.min(1.0, parsed));
        } catch (NumberFormatException exception) {
            return 0.0;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.trim().toLowerCase(), Normalizer.Form.NFKC)
                .replaceAll("\\s+", " ");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record Evaluation(boolean correct, boolean checkedByAi, double confidence, String reason) {
    }

    private record AiDecision(boolean correct, double confidence, String reason) {
    }
}
