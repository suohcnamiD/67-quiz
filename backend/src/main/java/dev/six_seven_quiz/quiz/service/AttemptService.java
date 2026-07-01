package dev.six_seven_quiz.quiz.service;

import dev.six_seven_quiz.quiz.component.mapper.*;
import dev.six_seven_quiz.quiz.dto.request.AttemptAction;
import dev.six_seven_quiz.quiz.dto.request.CommitAttemptActionsRequest;
import dev.six_seven_quiz.quiz.dto.request.FinishAttemptRequest;
import dev.six_seven_quiz.quiz.dto.response.attempt.AttemptInProgressDto;
import dev.six_seven_quiz.quiz.dto.response.attempt.AttemptQuestionDto;
import dev.six_seven_quiz.quiz.dto.response.attempt.AttemptOptionDto;
import dev.six_seven_quiz.quiz.dto.response.viewing.FinishedAttemptSummaryDto;
import dev.six_seven_quiz.quiz.dto.response.viewing.FinishedOptionDto;
import dev.six_seven_quiz.quiz.dto.response.viewing.FinishedQuestionDto;
import dev.six_seven_quiz.notification.model.NotificationType;
import dev.six_seven_quiz.notification.service.NotificationService;
import dev.six_seven_quiz.quiz.dto.response.viewing.QuizSummaryDto;
import dev.six_seven_quiz.quiz.dto.response.QuizRatingSummaryDto;
import dev.six_seven_quiz.quiz.exception.AttemptFinishedException;
import dev.six_seven_quiz.quiz.exception.AttemptNotFoundException;
import dev.six_seven_quiz.quiz.exception.NoAccessToAttemptException;
import dev.six_seven_quiz.quiz.exception.QuizNotFoundException;
import dev.six_seven_quiz.quiz.model.*;
import dev.six_seven_quiz.quiz.repository.AttemptQuestionRepository;
import dev.six_seven_quiz.quiz.repository.QuestionRepository;
import dev.six_seven_quiz.quiz.repository.QuizAttemptRepository;
import dev.six_seven_quiz.quiz.repository.QuizRatingRepository;
import dev.six_seven_quiz.quiz.repository.QuizRepository;
import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.ApplicationUserService;
import dev.six_seven_quiz.user.profile.component.mapper.UserProfileMapper;
import dev.six_seven_quiz.user.profile.dto.AuthorSummaryDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AttemptService {
    private final ApplicationUserService applicationUserService;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizRepository quizRepository;
    private final AttemptMapper attemptMapper;
    private final OptionMapper optionMapper;
    private final AttemptQuestionMapper attemptQuestionMapper;
    private static final int ATTEMPTS_PER_PAGE = 12;
    private final QuestionRepository questionRepository;
    private final QuizMapper quizMapper;
    private final UserProfileMapper userProfileMapper;
    private final QuizRatingRepository quizRatingRepository;
    private final NotificationService notificationService;

    private Pageable produceSanitizedPageable(int page) {
        // Newest attempts first — matches how users think about "my past
        // attempts" (last one taken should be at the top).
        return PageRequest.of(page, ATTEMPTS_PER_PAGE, Sort.by(Sort.Direction.DESC, "startedAt"));
    }

    @PersistenceContext
    private EntityManager entityManager;

    public AttemptService(ApplicationUserService applicationUserService, QuizAttemptRepository quizAttemptRepository, QuizRepository quizRepository, AttemptMapper attemptMapper, OptionMapper optionMapper, AttemptQuestionMapper attemptQuestionMapper, AttemptQuestionRepository attemptQuestionRepository, QuestionRepository questionRepository, QuizMapper quizMapper, UserProfileMapper userProfileMapper, QuizRatingRepository quizRatingRepository, NotificationService notificationService) {
        this.applicationUserService = applicationUserService;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizRepository = quizRepository;
        this.attemptMapper = attemptMapper;
        this.optionMapper = optionMapper;
        this.attemptQuestionMapper = attemptQuestionMapper;
        this.questionRepository = questionRepository;
        this.quizMapper = quizMapper;
        this.userProfileMapper = userProfileMapper;
        this.quizRatingRepository = quizRatingRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public AttemptInProgressDto commitAttemptActionsAsUser(UserDetails userDetails, CommitAttemptActionsRequest request) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        refreshFinishedAttempts(user);
        entityManager.flush();
        Attempt attempt = quizAttemptRepository.findById(request.attemptId()).orElseThrow(() -> new AttemptNotFoundException(request.attemptId()));
        validateUserAttemptOwnership(user, attempt);
        validateAttemptUnfinished(attempt);
        for (AttemptAction action : request.actions()) {
            if (action.selected()) attempt.selectOption(action.questionId(), action.optionId());
            else attempt.deselectOption(action.questionId(), action.optionId());
        }
        attempt = quizAttemptRepository.save(attempt);
        entityManager.flush();
        entityManager.refresh(attempt);

        return attemptToDto(attempt);
    }

    @Transactional
    public AttemptInProgressDto attemptQuizAsUser(UserDetails userDetails, UUID quizId) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new QuizNotFoundException(quizId));

        Attempt attempt = quiz.start(user);
        attempt = quizAttemptRepository.save(attempt);
        entityManager.flush();
        entityManager.refresh(attempt);
        return attemptToDto(attempt);

    }

    private QuizSummaryDto quizToSummary(Quiz quiz, ApplicationUser user) {
        // The attempts.quiz_id FK is ON DELETE SET NULL — when an author deletes
        // their quiz, every attempt of that quiz survives but loses its quiz
        // reference. Emit a tombstone DTO so old score history still renders.
        if (quiz == null) {
            return new QuizSummaryDto(
                    "Deleted quiz",
                    null,
                    0,
                    0,
                    java.time.Duration.ZERO,
                    null,
                    false,
                    false,
                    null,
                    new QuizRatingSummaryDto(null, 0L)
            );
        }
        int questionCount = questionRepository.countByQuiz_QuizId(quiz.getId());
        boolean areYouAuthor = quiz.getAuthor() != null && quiz.getAuthor().equals(user);
        long ratingCount = quizRatingRepository.countForQuiz(quiz.getId());
        Double ratingAvg = ratingCount > 0
                ? quizRatingRepository.averageScoreForQuiz(quiz.getId()).orElse(null)
                : null;
        QuizRatingSummaryDto ratingSummary = new QuizRatingSummaryDto(ratingAvg, ratingCount);
        AuthorSummaryDto authorSummary = quiz.getAuthor() != null
                ? userProfileMapper.toAuthorSummary(quiz.getAuthor())
                : null;
        return quizMapper.toSummary(quiz, questionCount, areYouAuthor, authorSummary, ratingSummary);
    }

    private void validateUserAttemptOwnership(ApplicationUser user, Attempt attempt) {
        if (!attempt.getUser().equals(user)) throw new NoAccessToAttemptException(attempt.getId());
    }

    @Transactional
    protected void validateAttemptUnfinished(Attempt attempt) {
        if (attempt.getFinishDeadline().isBefore(Instant.now())) attempt.finish();
        if (attempt.isFinished()) throw new AttemptFinishedException();
    }

    private FinishedAttemptSummaryDto attemptToFinishedSummary(Attempt attempt) {
        int maximumScore = attempt.getMaximumScore();
        int score = attempt.getEarnedScore();
        List<FinishedQuestionDto> finishedQuestions = new ArrayList<>();

        for (AttemptQuestion question : attempt.getQuestions()) {
            List<FinishedOptionDto> finishedOptions = question.getFinishedOptions();
            FinishedQuestionDto finishedQuestionDto = attemptQuestionMapper.toFinishedDto(question, finishedOptions);
            finishedQuestions.add(finishedQuestionDto);
        }

        return attemptMapper.toFinishedSummary(attempt, finishedQuestions, score, maximumScore, quizToSummary(attempt.getQuiz(), attempt.getUser()));
    }

    private AttemptInProgressDto attemptToDto(Attempt attempt) {
        List<AttemptQuestionDto> attemptQuestions = attempt.getQuestions().stream().map(question -> {
            List<Option> generalOptions = question.getOptions();
            List<Option> selectedOptions = question.getSelectedOptions();

            List<AttemptOptionDto> attemptOptionDtos = new ArrayList<>();

            for (Option option : generalOptions) {
                boolean selected = selectedOptions.contains(option);
                attemptOptionDtos.add(optionMapper.toDto(option, selected));
            }
            return attemptQuestionMapper.toDto(question, attemptOptionDtos);
        }).toList();
        return attemptMapper.toDto(attempt, attemptQuestions, quizToSummary(attempt.getQuiz(), attempt.getUser()));
    }

    @Transactional
    public Page<AttemptInProgressDto> getAttemptsInProgressAsUser(UserDetails userDetails, int page) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        // Filter past-deadline rows at read time rather than sweeping them
        // to finished=true. The sweep used to fire a bulk UPDATE on every
        // list call, which raced under MariaDB REPEATABLE_READ any time two
        // requests hit the same user's rows (poll + explicit finish, two
        // tabs, auto-finish + poll) and tripped Error 1020. The finish
        // endpoint still flips the flag idempotently under a pessimistic
        // lock — this read path just doesn't need to write.
        Page<Attempt> attempts = quizAttemptRepository.findActiveByUser(user, Instant.now(), produceSanitizedPageable(page));
        return attempts.map(this::attemptToDto);
    }

    @Transactional
    public Page<FinishedAttemptSummaryDto> getFinishedAttemptsAsUser(UserDetails userDetails, int page) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        Page<Attempt> attempts = quizAttemptRepository.findFinishedOrExpiredByUser(user, Instant.now(), produceSanitizedPageable(page));
        return attempts.map(this::attemptToFinishedSummary);
    }

    /**
     * Kept for the explicit finish path: promotes a single past-deadline
     * attempt to finished under a pessimistic write lock so no two callers
     * race. The list endpoints deliberately do NOT call this — see the
     * comment above them.
     */
    @Transactional
    protected void refreshFinishedAttempts(ApplicationUser user) {
        // No-op on read paths; kept as an anchor for the finish path which
        // handles the transition inline with a row-level lock.
    }

    @Transactional
    public FinishedAttemptSummaryDto finishAttemptAsUser(UserDetails userDetails, FinishAttemptRequest request) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        // Run the deadline sweep first so its bulk UPDATE + entityManager.clear()
        // can't strand a stale, managed copy of the attempt we're about to
        // mutate. Load the attempt after the sweep has settled, and take a
        // pessimistic write lock so a second finish (polling list, second tab,
        // auto-finish) waits for us instead of racing to flush and tripping
        // MariaDB's "record has changed since last read" error.
        refreshFinishedAttempts(user);
        Attempt attempt = quizAttemptRepository.findByIdForUpdate(request.attemptId())
                .orElseThrow(() -> new AttemptNotFoundException(request.attemptId()));
        validateUserAttemptOwnership(user, attempt);

        // Idempotent flip. If the attempt is already finished (concurrent
        // finisher, double-click, auto-finish beat us), skip the write and
        // return the current state — throwing AttemptFinishedException here
        // would surface a spurious error for what is really a success.
        boolean transitioned = false;
        if (!attempt.isFinished()) {
            attempt.finish();
            attempt = quizAttemptRepository.save(attempt);
            entityManager.flush();
            entityManager.refresh(attempt);
            transitioned = true;
        }

        // Notify the quiz author unless they took their own quiz. Only emit
        // when we actually flipped the attempt — otherwise a duplicate finish
        // would re-notify. The quiz can have been deleted between the attempt
        // start and finish (quiz_attempts.quiz_id is ON DELETE SET NULL), so
        // guard both the quiz and its author.
        if (transitioned) {
            Quiz finishedQuiz = attempt.getQuiz();
            ApplicationUser author = finishedQuiz != null ? finishedQuiz.getAuthor() : null;
            if (author != null && !author.getId().equals(user.getId())) {
                int maxScore = attempt.getMaximumScore();
                int score = attempt.getEarnedScore();
                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("actorUsername", user.getUsername());
                payload.put("actorDisplayName", user.getDisplayName());
                payload.put("quizId", finishedQuiz.getId().toString());
                payload.put("quizName", finishedQuiz.getName());
                payload.put("attemptId", attempt.getId().toString());
                payload.put("score", score);
                payload.put("maxScore", maxScore);
                notificationService.create(author, NotificationType.QUIZ_ATTEMPTED, payload);
            }
        }

        return attemptToFinishedSummary(attempt);
    }
}
