package interview_prep.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    Optional<AuthSession> findByToken(String token);

    @Query("select session from AuthSession session join fetch session.user where session.token = :token")
    Optional<AuthSession> findByTokenWithUser(@Param("token") String token);

    void deleteByToken(String token);

    @Modifying
    @Query("delete from AuthSession session where session.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
