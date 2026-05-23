package interview_prep.profile;

import interview_prep.auth.UserAccount;
import interview_prep.content.InterviewTest;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "test_id"}))
@Getter
@Setter
@NoArgsConstructor
public class FavoriteTest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private InterviewTest test;

    private Instant createdAt = Instant.now();

    public FavoriteTest(UserAccount user, InterviewTest test) {
        this.user = user;
        this.test = test;
    }
}
