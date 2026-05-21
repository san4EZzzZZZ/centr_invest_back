package interview_prep.attempt;

import interview_prep.content.Question;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class AttemptAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private TestAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Question question;

    @Column(nullable = false)
    private boolean correct;

    @Lob
    @Column(nullable = false)
    private String submittedAnswer;

    @Column(nullable = false)
    private Instant answeredAt = Instant.now();

    public AttemptAnswer(TestAttempt attempt, Question question, boolean correct, String submittedAnswer) {
        this.attempt = attempt;
        this.question = question;
        this.correct = correct;
        this.submittedAnswer = submittedAnswer;
    }
}
