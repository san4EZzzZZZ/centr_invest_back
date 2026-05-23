package interview_prep.attempt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AttemptController {
    private final AttemptService attemptService;
    private final AiReviewService aiReviewService;

    public AttemptController(AttemptService attemptService, AiReviewService aiReviewService) {
        this.attemptService = attemptService;
        this.aiReviewService = aiReviewService;
    }

    @PostMapping("/tests/{testId}/attempts")
    public AttemptDtos.StartAttemptResponse start(@PathVariable Long testId) {
        return attemptService.start(testId);
    }

    @GetMapping("/attempts/{attemptId}")
    public AttemptDtos.AttemptStateResponse state(@PathVariable Long attemptId) {
        return attemptService.state(attemptId);
    }

    @PostMapping("/attempts/{attemptId}/answer")
    public AttemptDtos.AnswerResponse answer(@PathVariable Long attemptId, @RequestBody AnswerRequest request) {
        return attemptService.answer(attemptId, request);
    }

    @GetMapping("/attempts/{attemptId}/result")
    public AttemptDtos.ResultResponse result(@PathVariable Long attemptId) {
        return attemptService.result(attemptId);
    }

    @GetMapping("/attempts/{attemptId}/ai-review")
    public AttemptDtos.AiReviewResponse aiReview(@PathVariable Long attemptId) {
        return aiReviewService.review(attemptId);
    }
}
