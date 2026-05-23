package interview_prep.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByProfessionIdOrderByPosition(Long professionId);

    List<Question> findByTestIdOrderByPosition(Long testId);

    List<Question> findByTestIdAndTypeOrderByPosition(Long testId, QuestionType type);

    long countByTestId(Long testId);
}
