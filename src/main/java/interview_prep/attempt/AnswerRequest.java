package interview_prep.attempt;

import java.util.List;
import java.util.Map;

public record AnswerRequest(
        List<Long> selectedOptionIds,
        Map<String, String> matches,
        String textAnswer
) {
}
