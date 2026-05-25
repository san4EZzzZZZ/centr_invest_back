package interview_prep.admin;

import interview_prep.content.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AdminDtos {
    public record TestUpsertRequest(
            Long languageId,
            Long professionId,
            @NotBlank String title,
            @NotBlank String shortDescription,
            @NotBlank String description,
            @Valid List<QuestionUpsertRequest> questions
    ) {
        public Long effectiveLanguageId() {
            return languageId != null ? languageId : professionId;
        }
    }

    public record QuestionUpsertRequest(
            @NotNull QuestionType type,
            @NotBlank String topic,
            @NotBlank String prompt,
            String correctTextAnswer,
            @Valid List<OptionUpsertRequest> options,
            @Valid List<MatchPairUpsertRequest> matchPairs
    ) {
    }

    public record OptionUpsertRequest(@NotBlank String text, boolean correct) {
    }

    public record MatchPairUpsertRequest(@NotBlank String leftLabel, @NotBlank String rightLabel) {
    }

    public record TestSummaryResponse(
            Long id,
            Long languageId,
            String languageTitle,
            Long createdById,
            String createdByEmail,
            String title,
            String shortDescription,
            String description,
            boolean published,
            int questionCount
    ) {
    }

    public record TestDetailsResponse(
            Long id,
            Long languageId,
            String languageTitle,
            Long createdById,
            String createdByEmail,
            String title,
            String shortDescription,
            String description,
            boolean published,
            List<QuestionDetailsResponse> questions
    ) {
    }

    public record QuestionDetailsResponse(
            Long id,
            int position,
            QuestionType type,
            String topic,
            String prompt,
            String correctTextAnswer,
            List<OptionDetailsResponse> options,
            List<MatchPairDetailsResponse> matchPairs
    ) {
    }

    public record OptionDetailsResponse(Long id, String text, boolean correct) {
    }

    public record MatchPairDetailsResponse(Long id, String leftLabel, String rightLabel) {
    }
}
