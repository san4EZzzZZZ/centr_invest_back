package interview_prep.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteTestRepository extends JpaRepository<FavoriteTest, Long> {
    boolean existsByUserIdAndTestId(Long userId, Long testId);

    Optional<FavoriteTest> findByUserIdAndTestId(Long userId, Long testId);

    List<FavoriteTest> findByUserIdOrderByCreatedAtDesc(Long userId);

    void deleteByTestId(Long testId);
}
