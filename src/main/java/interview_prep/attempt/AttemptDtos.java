package interview_prep.attempt;

import interview_prep.content.ContentDtos;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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
            List<Long> correctOptionIds,
            Map<String, Boolean> matchingResults,
            ContentDtos.QuestionResponse nextQuestion,
            ResultResponse result
    ) {
    }

    public record ResultResponse(
            Long attemptId,
            Long testId,
            String testTitle,
            int correctAnswers,
            int totalQuestions,
            String duration,
            String bestTime,
            List<String> weakTopics,
            String recommendation,
            AiReviewResponse aiReview,
            Instant completedAt
    ) {
    }

    public record RecentAttemptResponse(
            Long attemptId,
            Long testId,
            String languageTitle,
            String testTitle,
            int correctAnswers,
            int totalQuestions,
            String duration,
            String bestTime,
            Instant completedAt
    ) {
    }

    public record CompletedTestResponse(
            Long attemptId,
            Long testId,
            String languageTitle,
            String testTitle,
            int correctAnswers,
            int totalQuestions,
            String duration,
            String bestTime,
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
