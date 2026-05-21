package interview_prep.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class QuestionOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Question question;

    @Column(nullable = false, length = 500)
    private String text;

    @Column(nullable = false)
    private boolean correct;

    public QuestionOption(Question question, String text, boolean correct) {
        this.question = question;
        this.text = text;
        this.correct = correct;
    }
}
