package interview_prep.attempt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttemptQuestionRepository extends JpaRepository<AttemptQuestion, Long> {
    Optional<AttemptQuestion> findByAttemptIdAndPosition(Long attemptId, int position);

    List<AttemptQuestion> findByAttemptIdOrderByPosition(Long attemptId);

    void deleteByAttemptIdIn(List<Long> attemptIds);

    void deleteByQuestionId(Long questionId);
}
