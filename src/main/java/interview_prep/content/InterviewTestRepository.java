package interview_prep.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewTestRepository extends JpaRepository<InterviewTest, Long> {
    List<InterviewTest> findByProfessionIdOrderByTitle(Long professionId);
}
