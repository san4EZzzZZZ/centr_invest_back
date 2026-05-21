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
public class MatchPair {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Question question;

    @Column(nullable = false)
    private String leftLabel;

    @Column(nullable = false)
    private String rightLabel;

    public MatchPair(Question question, String leftLabel, String rightLabel) {
        this.question = question;
        this.leftLabel = leftLabel;
        this.rightLabel = rightLabel;
    }
}
