package interview_prep.content;

import interview_prep.auth.UserAccount;
import interview_prep.auth.UserAccountRepository;
import interview_prep.auth.UserRole;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataInitializer implements CommandLineRunner {
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

        if (professions.existsByTitle("Backend Java Developer")) {
            return;
        }

        Profession backend = professions.save(new Profession(
                "Backend Java Developer",
                "Базовая проверка знаний Java, HTTP, SQL, Spring и REST API для junior backend-разработчика."
        ));
        InterviewTest javaBasics = tests.save(new InterviewTest(
                backend,
                "Java Backend: базовое собеседование",
                "7 вопросов разных типов: один ответ, несколько ответов, соответствие и короткий текст."
        ));

        Question q1 = question(backend, 1, QuestionType.SINGLE_CHOICE, "HTTP",
                "Какой HTTP-метод обычно используют для получения ресурса без изменения состояния сервера?",
                null,
                "GET предназначен для чтения ресурса. Он должен быть безопасным: сам запрос не должен менять состояние сервера.",
                "https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/GET");
        options.save(new QuestionOption(q1, "GET", true));
        options.save(new QuestionOption(q1, "POST", false));
        options.save(new QuestionOption(q1, "PATCH", false));
        options.save(new QuestionOption(q1, "DELETE", false));

        Question q2 = question(backend, 2, QuestionType.MULTIPLE_CHOICE, "Java Collections",
                "Какие коллекции в Java обычно гарантируют уникальность элементов?",
                null,
                "Интерфейс Set описывает набор уникальных элементов. HashSet и TreeSet являются его распространенными реализациями.",
                "https://docs.oracle.com/javase/tutorial/collections/interfaces/set.html");
        options.save(new QuestionOption(q2, "ArrayList", false));
        options.save(new QuestionOption(q2, "HashSet", true));
        options.save(new QuestionOption(q2, "TreeSet", true));
        options.save(new QuestionOption(q2, "LinkedList", false));

        Question q3 = question(backend, 3, QuestionType.MATCHING, "SQL",
                "Сопоставь SQL-операцию с ее назначением.",
                null,
                "SELECT читает данные, INSERT добавляет строки, UPDATE изменяет существующие строки, DELETE удаляет строки.",
                "https://www.postgresql.org/docs/current/tutorial-sql.html");
        pairs.save(new MatchPair(q3, "SELECT", "Получение данных"));
        pairs.save(new MatchPair(q3, "INSERT", "Добавление строки"));
        pairs.save(new MatchPair(q3, "UPDATE", "Изменение строки"));
        pairs.save(new MatchPair(q3, "DELETE", "Удаление строки"));

        question(backend, 4, QuestionType.SHORT_TEXT, "Spring",
                "Как называется Spring-аннотация, которой обычно помечают класс REST-контроллера?",
                "@RestController",
                "@RestController объединяет @Controller и @ResponseBody, поэтому методы возвращают данные в тело HTTP-ответа.",
                "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-restcontroller.html");

        Question q5 = question(backend, 5, QuestionType.SINGLE_CHOICE, "JPA",
                "Что обычно означает аннотация @Entity в JPA?",
                null,
                "@Entity помечает класс как сущность, состояние которой может сохраняться в таблице базы данных.",
                "https://jakarta.ee/specifications/persistence/");
        options.save(new QuestionOption(q5, "Класс является Spring REST-контроллером", false));
        options.save(new QuestionOption(q5, "Класс является сохраняемой JPA-сущностью", true));
        options.save(new QuestionOption(q5, "Метод будет выполнен в транзакции", false));
        options.save(new QuestionOption(q5, "Поле нельзя записывать в базу", false));

        Question q6 = question(backend, 6, QuestionType.MULTIPLE_CHOICE, "REST",
                "Какие признаки обычно относятся к REST API?",
                null,
                "REST строится вокруг ресурсов, стандартных HTTP-методов и статeless-взаимодействия между клиентом и сервером.",
                "https://restfulapi.net/");
        options.save(new QuestionOption(q6, "Ресурсы имеют URI", true));
        options.save(new QuestionOption(q6, "Сервер хранит состояние UI каждого клиента", false));
        options.save(new QuestionOption(q6, "Используются стандартные HTTP-методы", true));
        options.save(new QuestionOption(q6, "Каждый запрос должен содержать достаточно контекста для обработки", true));

        question(backend, 7, QuestionType.SHORT_TEXT, "Java Core",
                "Какое ключевое слово в Java используется для наследования класса?",
                "extends",
                "Класс наследует другой класс с помощью extends. Для реализации интерфейса используется implements.",
                "https://docs.oracle.com/javase/tutorial/java/IandI/subclasses.html");
    }

    private Question question(Profession profession, int position, QuestionType type, String topic, String prompt,
                              String correctTextAnswer, String explanation, String readMoreUrl) {
        return questions.save(new Question(profession, position, type, topic, prompt, correctTextAnswer, explanation, readMoreUrl));
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
