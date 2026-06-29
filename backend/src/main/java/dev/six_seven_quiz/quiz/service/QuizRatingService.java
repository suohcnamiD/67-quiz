package dev.six_seven_quiz.quiz.service;

import dev.six_seven_quiz.quiz.component.mapper.QuizRatingMapper;
import dev.six_seven_quiz.quiz.dto.response.QuizRatingDto;
import dev.six_seven_quiz.quiz.exception.InvalidRatingException;
import dev.six_seven_quiz.quiz.exception.QuizNotFoundException;
import dev.six_seven_quiz.quiz.exception.RatingNotEligibleException;
import dev.six_seven_quiz.quiz.model.Quiz;
import dev.six_seven_quiz.quiz.model.QuizRating;
import dev.six_seven_quiz.quiz.repository.QuizAttemptRepository;
import dev.six_seven_quiz.quiz.repository.QuizRatingRepository;
import dev.six_seven_quiz.quiz.repository.QuizRepository;
import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.ApplicationUserService;
import dev.six_seven_quiz.user.profile.component.mapper.UserProfileMapper;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class QuizRatingService {

    public static final int RATINGS_PER_PAGE = 10;
    public static final int MIN_SCORE = 1;
    public static final int MAX_SCORE = 10;

    private final ApplicationUserService applicationUserService;
    private final QuizRepository quizRepository;
    private final QuizRatingRepository ratingRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizRatingMapper ratingMapper;
    private final UserProfileMapper userProfileMapper;

    public QuizRatingService(
            ApplicationUserService applicationUserService,
            QuizRepository quizRepository,
            QuizRatingRepository ratingRepository,
            QuizAttemptRepository attemptRepository,
            QuizRatingMapper ratingMapper,
            UserProfileMapper userProfileMapper
    ) {
        this.applicationUserService = applicationUserService;
        this.quizRepository = quizRepository;
        this.ratingRepository = ratingRepository;
        this.attemptRepository = attemptRepository;
        this.ratingMapper = ratingMapper;
        this.userProfileMapper = userProfileMapper;
    }

    @Transactional
    public QuizRatingDto upsert(UUID quizId, UserDetails userDetails, int score, String comment) {
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new InvalidRatingException(MIN_SCORE, MAX_SCORE);
        }
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new QuizNotFoundException(quizId));

        if (!attemptRepository.existsByUser_IdAndQuiz_IdAndFinishedIsTrue(user.getId(), quizId)) {
            throw new RatingNotEligibleException(quizId);
        }

        String trimmedComment = (comment == null || comment.isBlank()) ? null : comment.trim();

        QuizRating saved = ratingRepository.findByQuiz_IdAndUser_Id(quizId, user.getId())
                .map(existing -> {
                    existing.update(score, trimmedComment);
                    return existing;
                })
                .orElseGet(() -> ratingRepository.save(new QuizRating(quiz, user, score, trimmedComment)));

        return toDto(saved);
    }

    @Transactional
    public Optional<QuizRatingDto> getMine(UUID quizId, UserDetails userDetails) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        return ratingRepository.findByQuiz_IdAndUser_Id(quizId, user.getId()).map(this::toDto);
    }

    @Transactional
    public Page<QuizRatingDto> list(UUID quizId, int page) {
        if (!quizRepository.existsById(quizId)) {
            throw new QuizNotFoundException(quizId);
        }
        Pageable pageable = PageRequest.of(page, RATINGS_PER_PAGE);
        return ratingRepository.findByQuiz_IdOrderByCreatedAtDesc(quizId, pageable).map(this::toDto);
    }

    public Summary aggregate(UUID quizId) {
        long count = ratingRepository.countForQuiz(quizId);
        Double avg = ratingRepository.averageScoreForQuiz(quizId).orElse(null);
        return new Summary(avg, count);
    }

    private QuizRatingDto toDto(QuizRating rating) {
        return ratingMapper.toDto(rating, userProfileMapper.toAuthorSummary(rating.getUser()));
    }

    public record Summary(Double average, long count) {}
}
