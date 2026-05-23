package interview_prep.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Profession profession;

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType type;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false, length = 1200)
    private String prompt;

    @Column(length = 300)
    private String correctTextAnswer;

    @Lob
    @Column(nullable = false)
    private String explanation;

    @Column(nullable = false)
    private String readMoreUrl;

    public Question(Profession profession, int position, QuestionType type, String topic, String prompt,
                    String correctTextAnswer, String explanation, String readMoreUrl) {
        this.profession = profession;
        this.position = position;
        this.type = type;
        this.topic = topic;
        this.prompt = prompt;
        this.correctTextAnswer = correctTextAnswer;
        this.explanation = explanation;
        this.readMoreUrl = readMoreUrl;
    }
}
