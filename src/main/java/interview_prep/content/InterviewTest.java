package interview_prep.content;

import interview_prep.auth.UserAccount;
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
public class InterviewTest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Profession profession;

    @ManyToOne(fetch = FetchType.LAZY)
    private UserAccount createdBy;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 300)
    private String shortDescription;

    @Column(nullable = false, length = 800)
    private String description;

    public InterviewTest(Profession profession, String title, String shortDescription, String description) {
        this.profession = profession;
        this.title = title;
        this.shortDescription = shortDescription;
        this.description = description;
    }

    public InterviewTest(Profession profession, UserAccount createdBy, String title, String shortDescription, String description) {
        this(profession, title, shortDescription, description);
        this.createdBy = createdBy;
    }
}
