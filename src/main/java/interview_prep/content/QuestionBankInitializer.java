package interview_prep.content;

import interview_prep.auth.UserAccount;
import interview_prep.auth.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Order(2)
public class QuestionBankInitializer implements CommandLineRunner {
    private static final String BANK_FILE = "question-bank.tsv";

    private final ProfessionRepository professions;
    private final InterviewTestRepository tests;
    private final QuestionRepository questions;
    private final QuestionOptionRepository options;
    private final MatchPairRepository pairs;
    private final UserAccountRepository users;

    public QuestionBankInitializer(ProfessionRepository professions, InterviewTestRepository tests,
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
    public void run(String... args) throws Exception {
        ClassPathResource resource = new ClassPathResource(BANK_FILE);
        if (!resource.exists()) {
            return;
        }

        UserAccount owner = users.findByEmailIgnoreCase("admin@example.com").orElse(null);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                resource.getInputStream(),
                StandardCharsets.UTF_8
        ))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                BankQuestion row = BankQuestion.parse(line);
                Profession language = professions.findByTitle(row.language())
                        .orElseThrow(() -> new IllegalStateException("Language not found: " + row.language()));
                InterviewTest test = findOrCreateTest(language, owner, row.testTitle());
                if (questions.countByTestId(test.getId()) >= 21) {
                    continue;
                }
                saveQuestion(language, test, row);
            }
        }
    }

    private InterviewTest findOrCreateTest(Profession language, UserAccount owner, String title) {
        return tests.findByProfessionIdOrderByTitle(language.getId()).stream()
                .filter(test -> test.getTitle().equals(title))
                .findFirst()
                .orElseGet(() -> tests.save(new InterviewTest(
                        language,
                        owner,
                        title,
                        title,
                        title
                )));
    }

    private void saveQuestion(Profession language, InterviewTest test, BankQuestion row) {
        QuestionType type = QuestionType.valueOf(row.type());
        Question question = questions.save(new Question(
                language,
                test,
                row.position(),
                type,
                row.topic(),
                row.prompt(),
                type == QuestionType.SHORT_TEXT ? row.correct() : null
        ));

        if (type == QuestionType.SINGLE_CHOICE || type == QuestionType.MULTIPLE_CHOICE) {
            Map<String, String> correctByLabel = Arrays.stream(row.correct().split(","))
                    .map(String::trim)
                    .filter(label -> !label.isBlank())
                    .collect(Collectors.toMap(label -> label, label -> label));
            parseParts(row.options()).forEach(option -> {
                String[] parts = option.split("=", 2);
                options.save(new QuestionOption(
                        question,
                        parts[1],
                        correctByLabel.containsKey(parts[0])
                ));
            });
        }

        if (type == QuestionType.MATCHING) {
            parseParts(row.pairs()).forEach(pair -> {
                String[] parts = pair.split("=>", 2);
                pairs.save(new MatchPair(question, parts[0], parts[1]));
            });
        }
    }

    private List<String> parseParts(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\|"))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
    }

    private record BankQuestion(String language, String testTitle, int position, String type, String topic,
                                String prompt, String options, String correct, String pairs) {
        private static BankQuestion parse(String line) {
            String[] columns = line.split("\t", -1);
            if (columns.length != 9) {
                throw new IllegalArgumentException("Invalid question bank row: " + line);
            }
            return new BankQuestion(
                    columns[0],
                    columns[1],
                    Integer.parseInt(columns[2]),
                    columns[3],
                    columns[4],
                    columns[5],
                    columns[6],
                    columns[7],
                    columns[8]
            );
        }
    }
}
