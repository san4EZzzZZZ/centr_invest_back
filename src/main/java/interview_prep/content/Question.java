package interview_prep.content;

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

    @ManyToOne(fetch = FetchType.LAZY)
    private InterviewTest test;

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

    @Column(columnDefinition = "text")
    private String explanation;

    @Column
    private String readMoreUrl;

    public Question(Profession profession, InterviewTest test, int position, QuestionType type, String topic, String prompt,
                    String correctTextAnswer) {
        this.profession = profession;
        this.test = test;
        this.position = position;
        this.type = type;
        this.topic = topic;
        this.prompt = prompt;
        this.correctTextAnswer = correctTextAnswer;
    }

    public Question(Profession profession, int position, QuestionType type, String topic, String prompt,
                    String correctTextAnswer) {
        this(profession, null, position, type, topic, prompt, correctTextAnswer);
    }
}
