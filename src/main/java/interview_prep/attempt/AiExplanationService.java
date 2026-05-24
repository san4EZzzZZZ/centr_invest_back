package interview_prep.attempt;

import interview_prep.content.MatchPair;
import interview_prep.content.MatchPairRepository;
import interview_prep.content.Question;
import interview_prep.content.QuestionOption;
import interview_prep.content.QuestionOptionRepository;
import interview_prep.content.QuestionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiExplanationService {
    private final QuestionOptionRepository options;
    private final MatchPairRepository pairs;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public AiExplanationService(QuestionOptionRepository options,
                                MatchPairRepository pairs,
                                @Value("${app.ai.openai.base-url}") String baseUrl,
                                @Value("${app.ai.openai.api-key:}") String apiKey,
                                @Value("${app.ai.openai.model:}") String model) {
        this.options = options;
        this.pairs = pairs;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public GeneratedExplanation explain(Question question, AnswerRequest request, boolean correct) {
        if (apiKey == null || apiKey.isBlank() || model == null || model.isBlank()) {
            return fallback(question, correct);
        }

        try {
            GeneratedExplanation generated = parse(callModel(question, request, correct));
            if (!generated.explanation().isBlank() && !generated.readMoreUrl().isBlank()) {
                return generated;
            }
        } catch (RuntimeException ignored) {
            // Fallback keeps the answer endpoint stable when the provider is unavailable.
        }
        return fallback(question, correct);
    }

    private String callModel(Question question, AnswerRequest request, boolean correct) {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.2,
                "max_tokens", 350,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", """
                                        You generate explanations for a technical interview preparation app.
                                        Return exactly two lines in Russian:
                                        EXPLANATION|short explanation why the answer is correct or incorrect
                                        URL|one real public learning resource URL relevant to the topic
                                        Do not invent private or inaccessible URLs.
                                        """
                        ),
                        Map.of(
                                "role", "user",
                                "content", prompt(question, request, correct)
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

    private String prompt(Question question, AnswerRequest request, boolean correct) {
        return """
                Topic: %s
                Question type: %s
                Question: %s
                Correct answer: %s
                Student answer: %s
                Is student correct: %s
                """.formatted(
                safe(question.getTopic()),
                question.getType(),
                safe(question.getPrompt()),
                correctAnswer(question),
                submittedAnswer(request),
                correct
        );
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
        return safe(question.getCorrectTextAnswer());
    }

    private String submittedAnswer(AnswerRequest request) {
        return "selectedOptionIds=%s; matches=%s; textAnswer=%s".formatted(
                request.selectedOptionIds(),
                request.matches(),
                request.textAnswer()
        );
    }

    private GeneratedExplanation parse(String content) {
        String explanation = "";
        String url = "";
        if (content != null) {
            for (String line : content.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.regionMatches(true, 0, "EXPLANATION|", 0, "EXPLANATION|".length())) {
                    explanation = trimmed.substring("EXPLANATION|".length()).trim();
                } else if (trimmed.regionMatches(true, 0, "URL|", 0, "URL|".length())) {
                    url = trimmed.substring("URL|".length()).trim();
                }
            }
        }
        return new GeneratedExplanation(explanation, url, true);
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

    private GeneratedExplanation fallback(Question question, boolean correct) {
        String explanation = correct
                ? "Ответ засчитан. Повтори тему \"%s\" и проверь, что можешь объяснить решение своими словами.".formatted(question.getTopic())
                : "Ответ не засчитан. Вернись к теме \"%s\" и сравни свой ответ с базовым определением или правилом.".formatted(question.getTopic());
        return new GeneratedExplanation(explanation, fallbackUrl(question), false);
    }

    private String fallbackUrl(Question question) {
        String query = safe(question.getTopic()).replace(" ", "+");
        return "https://www.google.com/search?q=" + query + "+technical+interview";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record GeneratedExplanation(String explanation, String readMoreUrl, boolean generatedByAi) {
    }
}
