package interview_prep.content;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContentMapper {
    private final QuestionOptionRepository options;
    private final MatchPairRepository pairs;

    public ContentMapper(QuestionOptionRepository options, MatchPairRepository pairs) {
        this.options = options;
        this.pairs = pairs;
    }

    public ContentDtos.QuestionResponse toQuestionResponse(Question question) {
        List<ContentDtos.OptionResponse> optionResponses = options.findByQuestionIdOrderById(question.getId()).stream()
                .map(option -> new ContentDtos.OptionResponse(option.getId(), option.getText()))
                .toList();
        List<MatchPair> matchPairs = pairs.findByQuestionIdOrderById(question.getId());
        List<String> leftItems = matchPairs.stream()
                .map(MatchPair::getLeftLabel)
                .toList();
        List<String> rightItems = matchPairs.stream()
                .map(MatchPair::getRightLabel)
                .sorted()
                .toList();
        return new ContentDtos.QuestionResponse(
                question.getId(),
                question.getPosition(),
                question.getType(),
                question.getTopic(),
                question.getPrompt(),
                optionResponses,
                leftItems,
                rightItems
        );
    }
}
