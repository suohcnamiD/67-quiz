package dev.six_seven_quiz.shared.component;

import dev.six_seven_quiz.quiz.dto.QuestionData;
import dev.six_seven_quiz.quiz.dto.request.OptionData;
import dev.six_seven_quiz.quiz.model.QuestionType;
import dev.six_seven_quiz.quiz.model.Quiz;
import dev.six_seven_quiz.quiz.repository.QuizRepository;
import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.ApplicationUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Bootstraps the demo "sampler" user + two quizzes on every startup if they
 * are missing. The E2E suite logs in as sampler and consumes "Sampler 10" and
 * "Mixed types" — without this seeder a fresh database leaves 14 specs failing.
 *
 * Idempotent on the user: if sampler exists, the seeder does nothing. This
 * gates the whole block, so quizzes are only created in the same transaction
 * that creates the user.
 *
 * Implemented as an ApplicationRunner instead of a Liquibase changeset so the
 * password is hashed by the live PasswordEncoder bean rather than committing
 * a precomputed BCrypt string to the changelog.
 */
@Component
public class SamplerSeeder implements ApplicationRunner {

    private static final String USERNAME = "sampler";
    private static final String PASSWORD = "Passw0rd1";
    private static final String DISPLAY_NAME = "sampler";

    private final ApplicationUserRepository userRepository;
    private final QuizRepository quizRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    public SamplerSeeder(
            ApplicationUserRepository userRepository,
            QuizRepository quizRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.quizRepository = quizRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.findByUsername(USERNAME).isPresent()) {
            return;
        }

        ApplicationUser sampler = userRepository.save(
                new ApplicationUser(USERNAME, passwordEncoder.encode(PASSWORD), DISPLAY_NAME)
        );

        seedQuiz(sampler, "Sampler 10", samplerTenQuestions());
        seedQuiz(sampler, "Mixed types", mixedTypeQuestions());
    }

    private void seedQuiz(ApplicationUser author, String name, List<QuestionData> questions) {
        Quiz quiz = quizRepository.save(new Quiz(name, author, Duration.ofMinutes(5)));
        UUID quizId = quiz.getId();
        // Flush + clear so the next findById builds a fresh, fully-initialised
        // entity. Without this the persistence context returns the just-saved
        // object whose `questions` collection is still null (the Quiz ctor
        // doesn't initialise it; that normally only happens when Hibernate
        // hydrates an entity from the DB).
        entityManager.flush();
        entityManager.clear();
        Quiz managed = quizRepository.findById(quizId).orElseThrow();
        for (QuestionData q : questions) {
            managed.addQuestion(q);
        }
        quizRepository.save(managed);
    }

    private static List<QuestionData> samplerTenQuestions() {
        return List.of(
                multi("Which of these are prime numbers?",
                        opt("2", true), opt("4", false), opt("7", true), opt("9", false)),
                multi("Pick the planets in our solar system.",
                        opt("Mars", true), opt("Pluto", false), opt("Jupiter", true), opt("Krypton", false)),
                multi("Which are programming languages?",
                        opt("Python", true), opt("HTML", false), opt("Rust", true), opt("CSS", false)),
                multi("Continents on Earth — pick all.",
                        opt("Europe", true), opt("Atlantis", false), opt("Antarctica", true), opt("Pangaea", false)),
                multi("Which keys are on a standard QWERTY keyboard?",
                        opt("Q", true), opt("Ø", false), opt("Enter", true), opt("Æ", false))
        );
    }

    private static List<QuestionData> mixedTypeQuestions() {
        return List.of(
                single("The capital of France is…",
                        opt("Paris", true), opt("Lyon", false), opt("Berlin", false)),
                multi("Pick the even numbers.",
                        opt("2", true), opt("3", false), opt("4", true), opt("5", false)),
                single("HTTP status 404 means…",
                        opt("Not found", true), opt("Server error", false), opt("Unauthorized", false))
        );
    }

    private static QuestionData multi(String text, OptionData... options) {
        return new QuestionData(text, QuestionType.MULTI_CHOICE, List.of(options));
    }

    private static QuestionData single(String text, OptionData... options) {
        return new QuestionData(text, QuestionType.SINGLE_CHOICE, List.of(options));
    }

    private static OptionData opt(String text, boolean correct) {
        return new OptionData(text, correct);
    }
}
