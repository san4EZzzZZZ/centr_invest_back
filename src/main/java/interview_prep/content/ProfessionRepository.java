package interview_prep.content;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessionRepository extends JpaRepository<Profession, Long> {
    boolean existsByTitle(String title);
}
