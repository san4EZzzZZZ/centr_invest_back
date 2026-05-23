package interview_prep.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InterviewTestRepository extends JpaRepository<InterviewTest, Long> {
    List<InterviewTest> findByProfessionIdOrderByTitle(Long professionId);

    @Query("""
            select test from InterviewTest test
            join fetch test.profession profession
            where (:title is null or lower(test.title) like lower(concat('%', :title, '%')))
              and (:profession is null or lower(profession.title) like lower(concat('%', :profession, '%')))
            order by profession.title, test.title
            """)
    List<InterviewTest> search(@Param("title") String title, @Param("profession") String profession);
}
