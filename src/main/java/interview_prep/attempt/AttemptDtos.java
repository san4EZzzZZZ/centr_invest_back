package interview_prep.attempt;

import interview_prep.content.ContentDtos;

import java.time.Instant;
import java.util.List;

public class AttemptDtos {
    public record StartAttemptResponse(Long attemptId, ContentDtos.QuestionResponse question) {
    }

    public record AttemptStateResponse(
            Long attemptId,
            AttemptStatus status,
            int currentPosition,
            int totalQuestions,
            ContentDtos.QuestionResponse question
    ) {
    }

    public record AnswerResponse(
            boolean correct,
            String explanation,
            String readMoreUrl,
            boolean explanationGeneratedByAi,
            boolean checkedByAi,
            Double aiConfidence,
            ContentDtos.QuestionResponse nextQuestion,
            ResultResponse result
    ) {
    }

    public record ResultResponse(
            Long attemptId,
            String testTitle,
            int correctAnswers,
            int totalQuestions,
            List<String> weakTopics,
            String recommendation,
            AiReviewResponse aiReview,
            Instant completedAt
    ) {
    }

    public record RecentAttemptResponse(
            Long attemptId,
            String languageTitle,
            String testTitle,
            int correctAnswers,
            int totalQuestions,
            Instant completedAt
    ) {
    }

    public record AiReviewResponse(
            Long attemptId,
            boolean generatedByAi,
            String summary,
            List<AiTopicReview> topics,
            List<AiResource> resources,
            String nextStep
    ) {
    }

    public record AiTopicReview(
            String topic,
            String diagnosis,
            String recommendation
    ) {
    }

    public record AiResource(
            String title,
            String url,
            String reason
    ) {
    }
}
