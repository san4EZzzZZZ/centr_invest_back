package interview_prep.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {
    List<QuestionOption> findByQuestionIdOrderById(Long questionId);

    void deleteByQuestionId(Long questionId);
}
