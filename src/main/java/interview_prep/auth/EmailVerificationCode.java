package interview_prep.auth;

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
public class EmailVerificationCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationPurpose purpose;

    @ManyToOne(fetch = FetchType.LAZY)
    private UserAccount user;

    @Column(nullable = false)
    private String email;

    @Column
    private String newEmail;

    @Column
    private String username;

    @Column
    private String passwordHash;

    @Column(nullable = false)
    private String codeHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column
    private Instant consumedAt;

    public EmailVerificationCode(VerificationPurpose purpose, UserAccount user, String email, String newEmail,
                                 String username, String passwordHash, String codeHash, Instant expiresAt) {
        this.purpose = purpose;
        this.user = user;
        this.email = email;
        this.newEmail = newEmail;
        this.username = username;
        this.passwordHash = passwordHash;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }
}
