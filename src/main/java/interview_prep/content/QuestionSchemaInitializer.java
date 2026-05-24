package interview_prep.content;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class QuestionSchemaInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public QuestionSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("alter table if exists question alter column explanation drop not null");
            jdbcTemplate.execute("alter table if exists question alter column read_more_url drop not null");
        } catch (RuntimeException ignored) {
            // The entity mapping is already nullable. This only smooths migration for existing PostgreSQL databases.
        }
    }
}
