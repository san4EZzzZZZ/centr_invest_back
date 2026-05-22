package interview_prep.attempt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {
    List<TestAttempt> findTop5ByUserIdAndStatusOrderByStartedAtDesc(Long userId, AttemptStatus status);

    List<TestAttempt> findByTestId(Long testId);

    void deleteByTestId(Long testId);
}
