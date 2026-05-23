package interview_prep.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfessionRepository extends JpaRepository<Profession, Long> {
    boolean existsByTitle(String title);

    Optional<Profession> findByTitle(String title);
}
