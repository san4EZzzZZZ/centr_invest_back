package interview_prep.content;

import java.util.List;

public class ContentDtos {
    public record LanguageResponse(Long id, String title, String description, List<TestSummary> tests) {
    }

    public record TestSummary(Long id, String title, String shortDescription, String description, int questionCount,
                              boolean favorite) {
    }

    public record TestResponse(Long id, Long languageId, String title, String shortDescription, String description,
                               List<QuestionResponse> questions) {
    }

    public record QuestionResponse(
            Long id,
            int position,
            QuestionType type,
            String topic,
            String prompt,
            List<OptionResponse> options,
            List<String> matchLeftItems,
            List<String> matchRightItems
    ) {
    }

    public record OptionResponse(Long id, String text) {
    }
}
