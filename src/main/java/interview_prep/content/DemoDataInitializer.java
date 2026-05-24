package interview_prep.content;

import interview_prep.auth.UserAccount;
import interview_prep.auth.UserAccountRepository;
import interview_prep.auth.UserRole;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
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
    private final QuestionOptionRepository options;
    private final MatchPairRepository pairs;
    private final UserAccountRepository users;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DemoDataInitializer(ProfessionRepository professions, InterviewTestRepository tests,
                               QuestionRepository questions, QuestionOptionRepository options,
                               MatchPairRepository pairs, UserAccountRepository users) {
        this.professions = professions;
        this.tests = tests;
        this.questions = questions;
        this.options = options;
        this.pairs = pairs;
        this.users = users;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedLanguages();
        migrateLegacyJavaProfession();
        seedJavaDemoTest();
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

    private void seedJavaDemoTest() {
        Profession java = professions.findByTitle("Java")
                .orElseThrow(() -> new IllegalStateException("Java language category was not seeded"));

        InterviewTest javaBasics = tests.findByProfessionIdOrderByTitle(java.getId()).stream()
                .filter(test -> test.getTitle().equals("Junior Java Backend-разработчик"))
                .findFirst()
                .orElseGet(() -> tests.save(new InterviewTest(
                        java,
                        "Junior Java Backend-разработчик",
                        "Базовая проверка Java backend: HTTP, Java Core, SQL, JPA, Spring и REST.",
                        "Пул вопросов для подготовки к junior Java backend собеседованию. При старте попытки сервер выбирает до 7 вопросов нужных типов из пула этого теста."
                )));

        if (questions.countByTestId(javaBasics.getId()) > 0) {
            attachLegacyQuestionsToTest(javaBasics);
            return;
        }

        Question q1 = question(java, javaBasics, 1, QuestionType.SINGLE_CHOICE, "HTTP",
                "Какой HTTP-метод обычно используют для получения ресурса без изменения состояния сервера?",
                null);
        options.save(new QuestionOption(q1, "GET", true));
        options.save(new QuestionOption(q1, "POST", false));
        options.save(new QuestionOption(q1, "PATCH", false));
        options.save(new QuestionOption(q1, "DELETE", false));

        Question q2 = question(java, javaBasics, 2, QuestionType.SINGLE_CHOICE, "JPA",
                "Что обычно означает аннотация @Entity в JPA?",
                null);
        options.save(new QuestionOption(q2, "Класс является REST-контроллером", false));
        options.save(new QuestionOption(q2, "Класс является сохраняемой JPA-сущностью", true));
        options.save(new QuestionOption(q2, "Метод выполняется в транзакции", false));
        options.save(new QuestionOption(q2, "Поле нельзя записывать в базу", false));

        Question q3 = question(java, javaBasics, 3, QuestionType.MULTIPLE_CHOICE, "Java Collections",
                "Какие коллекции Java обычно гарантируют уникальность элементов?",
                null);
        options.save(new QuestionOption(q3, "ArrayList", false));
        options.save(new QuestionOption(q3, "HashSet", true));
        options.save(new QuestionOption(q3, "TreeSet", true));
        options.save(new QuestionOption(q3, "LinkedList", false));

        Question q4 = question(java, javaBasics, 4, QuestionType.MULTIPLE_CHOICE, "REST",
                "Какие признаки обычно относятся к REST API?",
                null);
        options.save(new QuestionOption(q4, "Ресурсы имеют URI", true));
        options.save(new QuestionOption(q4, "Сервер хранит состояние UI каждого клиента", false));
        options.save(new QuestionOption(q4, "Используются стандартные HTTP-методы", true));
        options.save(new QuestionOption(q4, "Каждый запрос содержит достаточно контекста для обработки", true));

        Question q5 = question(java, javaBasics, 5, QuestionType.MATCHING, "SQL",
                "Сопоставь SQL-операцию с ее назначением.",
                null);
        pairs.save(new MatchPair(q5, "SELECT", "Получение данных"));
        pairs.save(new MatchPair(q5, "INSERT", "Добавление строки"));
        pairs.save(new MatchPair(q5, "UPDATE", "Изменение строки"));
        pairs.save(new MatchPair(q5, "DELETE", "Удаление строки"));

        question(java, javaBasics, 6, QuestionType.SHORT_TEXT, "Spring",
                "Как называется Spring-аннотация, которой обычно помечают класс REST-контроллера?",
                "@RestController");

        question(java, javaBasics, 7, QuestionType.SHORT_TEXT, "Java Core",
                "Какое ключевое слово в Java используется для наследования класса?",
                "extends");
    }

    private Question question(Profession language, InterviewTest test, int position, QuestionType type, String topic,
                              String prompt, String correctTextAnswer) {
        return questions.save(new Question(language, test, position, type, topic, prompt, correctTextAnswer));
    }

    private void attachLegacyQuestionsToTest(InterviewTest test) {
        questions.findByProfessionIdOrderByPosition(test.getProfession().getId()).stream()
                .filter(question -> question.getTest() == null)
                .forEach(question -> {
                    question.setTest(test);
                    questions.save(question);
                });
    }

    private void seedAdmin() {
        if (users.existsByEmailIgnoreCase("admin@example.com")) {
            return;
        }

        users.save(new UserAccount(
                "admin@example.com",
                "Admin",
                passwordEncoder.encode("admin123"),
                UserRole.ADMIN
        ));
    }
}
