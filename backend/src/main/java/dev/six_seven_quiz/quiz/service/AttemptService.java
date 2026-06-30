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
    private static final int ATTEMPTS_PER_PAGE = 10;
    private final QuestionRepository questionRepository;
    private final QuizMapper quizMapper;
    private final UserProfileMapper userProfileMapper;
    private final QuizRatingRepository quizRatingRepository;
    private final NotificationService notificationService;

    private Pageable produceSanitizedPageable(int page) {
        return PageRequest.of(page, ATTEMPTS_PER_PAGE);
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
        refreshFinishedAttempts(user);
        entityManager.flush();
        Page<Attempt> attempts = quizAttemptRepository.findByUserAndFinishedIsFalse(user, produceSanitizedPageable(page));
        return attempts.map(this::attemptToDto);
    }

    @Transactional
    public Page<FinishedAttemptSummaryDto> getFinishedAttemptsAsUser(UserDetails userDetails, int page) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        refreshFinishedAttempts(user);
        entityManager.flush();
        Page<Attempt> attempts = quizAttemptRepository.findByUserAndFinishedIsTrue(user, produceSanitizedPageable(page));
        return attempts.map(this::attemptToFinishedSummary);
    }

    @Transactional
    protected void refreshFinishedAttempts(ApplicationUser user) {
        List<Attempt> unfinishedAttemptsPastDeadline = quizAttemptRepository.findByUser_IdAndFinishedIsFalseAndFinishDeadlineBefore(user.getId(), Instant.now());
        unfinishedAttemptsPastDeadline.forEach(Attempt::finish);
    }

    @Transactional
    public FinishedAttemptSummaryDto finishAttemptAsUser(UserDetails userDetails, FinishAttemptRequest request) {
        Attempt attempt = quizAttemptRepository.findById(request.attemptId()).orElseThrow(() -> new AttemptNotFoundException(request.attemptId()));
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        validateUserAttemptOwnership(user, attempt);
        refreshFinishedAttempts(user);
        entityManager.flush();
        validateAttemptUnfinished(attempt);

        attempt.finish();

        attempt = quizAttemptRepository.save(attempt);
        entityManager.flush();
        entityManager.refresh(attempt);

        // Notify the quiz author unless they took their own quiz.
        ApplicationUser author = attempt.getQuiz().getAuthor();
        if (!author.getId().equals(user.getId())) {
            int maxScore = attempt.getMaximumScore();
            int score = attempt.getEarnedScore();
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("actorUsername", user.getUsername());
            payload.put("actorDisplayName", user.getDisplayName());
            payload.put("quizId", attempt.getQuiz().getId().toString());
            payload.put("quizName", attempt.getQuiz().getName());
            payload.put("attemptId", attempt.getId().toString());
            payload.put("score", score);
            payload.put("maxScore", maxScore);
            notificationService.create(author, NotificationType.QUIZ_ATTEMPTED, payload);
        }

        return attemptToFinishedSummary(attempt);
    }
}
