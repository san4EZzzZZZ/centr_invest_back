package interview_prep.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteTestRepository extends JpaRepository<FavoriteTest, Long> {
    boolean existsByUserIdAndTestId(Long userId, Long testId);

    Optional<FavoriteTest> findByUserIdAndTestId(Long userId, Long testId);

    List<FavoriteTest> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
            select favorite from FavoriteTest favorite
            join fetch favorite.test test
            join fetch test.profession profession
            where favorite.user.id = :userId
              and (:title is null or lower(test.title) like lower(concat('%', :title, '%')))
              and (:language is null or lower(profession.title) like lower(concat('%', :language, '%')))
            order by favorite.createdAt desc
            """)
    List<FavoriteTest> searchByUserId(@Param("userId") Long userId,
                                      @Param("title") String title,
                                      @Param("language") String language);

    void deleteByTestId(Long testId);
}
