package dev.six_seven_quiz.quiz.service;

import dev.six_seven_quiz.quiz.component.mapper.QuizMapper;
import dev.six_seven_quiz.quiz.dto.request.CreateQuizRequest;
import dev.six_seven_quiz.quiz.dto.response.QuizRatingSummaryDto;
import dev.six_seven_quiz.quiz.dto.response.authoring.QuizDto;
import dev.six_seven_quiz.quiz.dto.response.viewing.QuizSummaryDto;
import dev.six_seven_quiz.quiz.exception.QuizNotFoundException;
import dev.six_seven_quiz.quiz.model.Quiz;
import dev.six_seven_quiz.quiz.repository.QuestionRepository;
import dev.six_seven_quiz.quiz.repository.QuizAttemptRepository;
import dev.six_seven_quiz.quiz.repository.QuizRatingRepository;
import dev.six_seven_quiz.quiz.repository.QuizRepository;
import dev.six_seven_quiz.quiz.validator.QuizValidator;
import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.ApplicationUserService;
import dev.six_seven_quiz.user.profile.component.mapper.UserProfileMapper;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class QuizService {

    // 21 instead of 20 so a 3-column desktop grid fills exactly 7 rows with no
    // trailing single-card row. Mobile (1-col) and tablet (2-col) layouts
    // don't care.
    private static final int QUIZZES_PER_PAGE = 21;

    private final ApplicationUserService applicationUserService;
    private final QuizRepository quizRepository;
    private final QuizMapper quizMapper;
    private final QuestionRepository questionRepository;
    private final UserProfileMapper userProfileMapper;
    private final QuizRatingRepository quizRatingRepository;

    public QuizService(ApplicationUserService applicationUserService, QuizRepository quizRepository, QuizMapper quizMapper, QuizAttemptRepository quizAttemptRepository, QuestionRepository questionRepository, UserProfileMapper userProfileMapper, QuizRatingRepository quizRatingRepository) {
        this.applicationUserService = applicationUserService;
        this.quizRepository = quizRepository;
        this.quizMapper = quizMapper;
        this.questionRepository = questionRepository;
        this.userProfileMapper = userProfileMapper;
        this.quizRatingRepository = quizRatingRepository;
    }

    public QuizDto createQuiz(UserDetails userDetails, CreateQuizRequest request) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);

        Quiz rawNewQuiz = new Quiz(request.quizName(), user, request.quizDuration());
        rawNewQuiz.setAuthor(user);

        Quiz newQuiz = quizRepository.save(rawNewQuiz);
        return this.quizToDto(newQuiz, user);
    }

    public Page<QuizSummaryDto> getQuizzes(int page, UserDetails userDetails) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        return quizRepository.findAll(produceSanitizedPageable(page)).map(quiz -> quizToSummary(quiz, user));
    }

    /**
     * Quizzes authored by {@code author}. {@code viewer} drives the
     * {@code youAreAuthor} flag on the summary — so the same listing reads
     * differently on someone else's profile vs. your own.
     */
    public Page<QuizSummaryDto> getQuizzesByAuthor(ApplicationUser author, ApplicationUser viewer, int page) {
        return quizRepository
                .findByAuthorOrderByNameAsc(author, produceSanitizedPageable(page))
                .map(quiz -> quizToSummary(quiz, viewer));
    }

    /**
     * Substring search on quiz name. {@code viewer} is used the same way as
     * above to populate {@code youAreAuthor}.
     */
    public Page<QuizSummaryDto> searchQuizzesByName(String needle, ApplicationUser viewer, int page, int pageSize) {
        return quizRepository
                .findByNameContainingIgnoreCaseOrderByNameAsc(needle, PageRequest.of(page, pageSize))
                .map(quiz -> quizToSummary(quiz, viewer));
    }

    private QuizDto quizToDto(Quiz quiz, ApplicationUser user) {
        int questionCount = questionRepository.countByQuiz_QuizId(quiz.getId());
        boolean areYouAuthor = quiz.getAuthor().equals(user);
        return quizMapper.toDto(quiz, questionCount, areYouAuthor);
    }

    private QuizSummaryDto quizToSummary(Quiz quiz, ApplicationUser user) {
        int questionCount = questionRepository.countByQuiz_QuizId(quiz.getId());
        boolean areYouAuthor = quiz.getAuthor().equals(user);
        long ratingCount = quizRatingRepository.countForQuiz(quiz.getId());
        Double ratingAvg = ratingCount > 0
                ? quizRatingRepository.averageScoreForQuiz(quiz.getId()).orElse(null)
                : null;
        QuizRatingSummaryDto ratingSummary = new QuizRatingSummaryDto(ratingAvg, ratingCount);
        return quizMapper.toSummary(quiz, questionCount, areYouAuthor, userProfileMapper.toAuthorSummary(quiz.getAuthor()), ratingSummary);
    }

    private Pageable produceSanitizedPageable(int page) {
        return PageRequest.of(page, QUIZZES_PER_PAGE);
    }

    @Transactional
    public void deleteAsUser(UUID quizId, UserDetails userDetails) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new QuizNotFoundException(quizId));
        QuizValidator.requireOwner(quiz, user);

        quizRepository.delete(quiz);
    }

    public QuizDto getAsAuthor(@NotNull UUID quizId, UserDetails userDetails) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new QuizNotFoundException(quizId));
        QuizValidator.requireOwner(quiz, user);
        return this.quizToDto(quiz, user);
    }
}
