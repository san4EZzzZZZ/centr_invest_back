package interview_prep.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchPairRepository extends JpaRepository<MatchPair, Long> {
    List<MatchPair> findByQuestionIdOrderById(Long questionId);
}
