package interview_prep.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final String from;

    public MailService(JavaMailSender mailSender, MailProperties mailProperties,
                       @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.from = from;
    }

    public void sendCode(String to, String subject, String code) {
        if (mailProperties.getHost() == null || mailProperties.getHost().isBlank()) {
            log.info("Email code for {} [{}]: {}", to, subject, code);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText("""
                Код подтверждения: %s

                Если вы не запрашивали это действие, просто проигнорируйте письмо.
                """.formatted(code));
        mailSender.send(message);
    }
}
