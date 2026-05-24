package interview_prep.content;

import interview_prep.auth.UserAccount;
import interview_prep.auth.UserAccountRepository;
import interview_prep.auth.UserRole;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@Order(1)
public class DemoDataInitializer implements CommandLineRunner {
    public static final List<String> LANGUAGE_TITLES = List.of(
            "Python",
            "Java",
            "C++",
            "C#",
            "SQL",
            "PHP",
            "JavaScript",
            "GO"
    );

    private static final Map<String, String> LANGUAGE_DESCRIPTIONS = Map.of(
            "Python", "Тесты для junior-ролей, где Python используется в backend, аналитике, автоматизации и full-stack разработке.",
            "Java", "Тесты для junior Java-разработчиков: core, backend, Spring, SQL и REST API.",
            "C++", "Тесты для junior C++-разработчиков: синтаксис, память, STL, ООП и базовые алгоритмы.",
            "C#", "Тесты для junior C#/.NET-разработчиков: язык, LINQ, ASP.NET, базы данных и ООП.",
            "SQL", "Тесты для ролей, где важны запросы, модели данных, JOIN, агрегации и индексы.",
            "PHP", "Тесты для junior PHP-разработчиков: язык, веб, Composer, Laravel и работа с БД.",
            "JavaScript", "Тесты для frontend, full-stack и Node.js junior-ролей.",
            "GO", "Тесты для junior Go-разработчиков: синтаксис, goroutines, HTTP, ошибки и работа с данными."
    );

    private final ProfessionRepository professions;
    private final InterviewTestRepository tests;
    private final QuestionRepository questions;
    private final UserAccountRepository users;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DemoDataInitializer(ProfessionRepository professions, InterviewTestRepository tests,
                               QuestionRepository questions, UserAccountRepository users) {
        this.professions = professions;
        this.tests = tests;
        this.questions = questions;
        this.users = users;
    }

    @Override
    @Transactional
    public void run(String... args) {
        UserAccount superAdmin = seedSuperAdmin();
        seedLanguages();
        migrateLegacyJavaProfession();
        assignLegacyTestsToSuperAdmin(superAdmin);
    }

    private void seedLanguages() {
        LANGUAGE_TITLES.forEach(title -> professions.findByTitle(title)
                .ifPresentOrElse(
                        language -> {
                            language.setDescription(LANGUAGE_DESCRIPTIONS.get(title));
                            professions.save(language);
                        },
                        () -> professions.save(new Profession(title, LANGUAGE_DESCRIPTIONS.get(title)))
                ));
    }

    private void migrateLegacyJavaProfession() {
        Profession java = professions.findByTitle("Java")
                .orElseThrow(() -> new IllegalStateException("Java language category was not seeded"));
        professions.findByTitle("Backend Java Developer").ifPresent(legacy -> {
            tests.findByProfessionIdOrderByTitle(legacy.getId()).forEach(test -> {
                test.setProfession(java);
                tests.save(test);
            });
            questions.findByProfessionIdOrderByPosition(legacy.getId()).forEach(question -> {
                question.setProfession(java);
                questions.save(question);
            });
            if (tests.countByProfessionId(legacy.getId()) == 0) {
                professions.delete(legacy);
            }
        });
    }

    private void assignLegacyTestsToSuperAdmin(UserAccount superAdmin) {
        tests.findAll().stream()
                .filter(test -> test.getCreatedBy() == null)
                .forEach(test -> {
                    test.setCreatedBy(superAdmin);
                    tests.save(test);
                });
    }

    private UserAccount seedSuperAdmin() {
        return users.findByEmailIgnoreCase("admin@example.com")
                .map(user -> {
                    if (user.getRole() != UserRole.SUPER_ADMIN) {
                        user.setRole(UserRole.SUPER_ADMIN);
                        return users.save(user);
                    }
                    return user;
                })
                .orElseGet(() -> users.save(new UserAccount(
                        "admin@example.com",
                        "Admin",
                        passwordEncoder.encode("admin123"),
                        UserRole.SUPER_ADMIN
                )));
    }
}
