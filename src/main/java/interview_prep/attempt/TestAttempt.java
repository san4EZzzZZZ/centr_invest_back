package interview_prep.attempt;

import interview_prep.auth.UserAccount;
import interview_prep.content.InterviewTest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class TestAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private InterviewTest test;

    @Column(nullable = false)
    private int currentPosition = 1;

    @Column(nullable = false)
    private int totalQuestions;

    @Column(nullable = false)
    private int correctAnswers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column(nullable = false)
    private Instant startedAt = Instant.now();

    private Instant completedAt;

    private Long durationSeconds;

    public TestAttempt(UserAccount user, InterviewTest test, int totalQuestions) {
        this.user = user;
        this.test = test;
        this.totalQuestions = totalQuestions;
    }
}
