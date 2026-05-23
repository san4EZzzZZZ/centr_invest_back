package interview_prep.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByProfessionIdOrderByPosition(Long professionId);

    List<Question> findByProfessionIdAndTypeOrderByPosition(Long professionId, QuestionType type);

    long countByProfessionId(Long professionId);

    Optional<Question> findFirstByProfessionIdAndPosition(Long professionId, int position);
}
