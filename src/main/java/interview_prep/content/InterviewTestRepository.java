package interview_prep.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InterviewTestRepository extends JpaRepository<InterviewTest, Long> {
    List<InterviewTest> findByProfessionIdOrderByTitle(Long professionId);

    long countByProfessionId(Long professionId);

    List<InterviewTest> findByCreatedById(Long createdById);

    @Query("""
            select test from InterviewTest test
            join fetch test.profession profession
            order by profession.title, test.title
            """)
    List<InterviewTest> findAllWithLanguage();

    @Query("""
            select test from InterviewTest test
            join fetch test.profession profession
            where lower(test.title) like lower(concat('%', :title, '%'))
            order by profession.title, test.title
            """)
    List<InterviewTest> searchByTitle(@Param("title") String title);

    @Query("""
            select test from InterviewTest test
            join fetch test.profession profession
            where lower(profession.title) like lower(concat('%', :profession, '%'))
            order by profession.title, test.title
            """)
    List<InterviewTest> searchByLanguage(@Param("profession") String profession);

    @Query("""
            select test from InterviewTest test
            join fetch test.profession profession
            where lower(test.title) like lower(concat('%', :title, '%'))
              and lower(profession.title) like lower(concat('%', :profession, '%'))
            order by profession.title, test.title
            """)
    List<InterviewTest> searchByTitleAndLanguage(@Param("title") String title,
                                                 @Param("profession") String profession);
}
