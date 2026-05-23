package interview_prep.attempt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {
    boolean existsByAttemptIdAndQuestionId(Long attemptId, Long questionId);

    Optional<AttemptAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);

    List<AttemptAnswer> findByAttemptIdOrderByQuestionPosition(Long attemptId);

    void deleteByAttemptIdIn(List<Long> attemptIds);

    void deleteByQuestionId(Long questionId);
}
