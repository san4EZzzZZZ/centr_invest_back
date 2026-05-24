package interview_prep.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    boolean existsByEmailIgnoreCase(String email);

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    List<UserAccount> findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCaseOrderByCreatedAtDesc(
            String email, String username);

    List<UserAccount> findAllByOrderByCreatedAtDesc();
}
