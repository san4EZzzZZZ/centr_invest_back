package interview_prep.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class UserRoleSchemaInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public UserRoleSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("alter table if exists users drop constraint if exists users_role_check");
        jdbcTemplate.execute("""
                alter table if exists users
                add constraint users_role_check
                check (role in ('USER', 'ADMIN', 'SUPER_ADMIN'))
                """);
    }
}
