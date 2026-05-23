package interview_prep.content;

import java.util.List;

public class ContentDtos {
    public record ProfessionResponse(Long id, String title, String description, List<TestSummary> tests) {
    }

    public record TestSummary(Long id, String title, String shortDescription, String description, int questionCount) {
    }

    public record TestResponse(Long id, Long professionId, String title, String shortDescription, String description,
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
