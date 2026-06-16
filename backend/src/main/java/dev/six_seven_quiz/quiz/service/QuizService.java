package dev.six_seven_quiz.quiz.service;

import dev.six_seven_quiz.quiz.component.mapper.QuizMapper;
import dev.six_seven_quiz.quiz.dto.request.CreateQuizRequest;
import dev.six_seven_quiz.quiz.dto.response.authoring.QuizDto;
import dev.six_seven_quiz.quiz.dto.response.viewing.QuizSummaryDto;
import dev.six_seven_quiz.quiz.exception.QuizNotFoundException;
import dev.six_seven_quiz.quiz.model.Quiz;
import dev.six_seven_quiz.quiz.repository.QuestionRepository;
import dev.six_seven_quiz.quiz.repository.QuizAttemptRepository;
import dev.six_seven_quiz.quiz.repository.QuizRepository;
import dev.six_seven_quiz.quiz.validator.QuizValidator;
import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.ApplicationUserService;
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

    private static final int QUIZZES_PER_PAGE = 20;

    private final ApplicationUserService applicationUserService;
    private final QuizRepository quizRepository;
    private final QuizMapper quizMapper;
    private final QuestionRepository questionRepository;

    public QuizService(ApplicationUserService applicationUserService, QuizRepository quizRepository, QuizMapper quizMapper, QuizAttemptRepository quizAttemptRepository, QuestionRepository questionRepository) {
        this.applicationUserService = applicationUserService;
        this.quizRepository = quizRepository;
        this.quizMapper = quizMapper;
        this.questionRepository = questionRepository;
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

    private QuizDto quizToDto(Quiz quiz, ApplicationUser user) {
        int questionCount = questionRepository.countByQuiz_QuizId(quiz.getId());
        boolean areYouAuthor = quiz.getAuthor().equals(user);
        return quizMapper.toDto(quiz, questionCount, areYouAuthor);
    }

    private QuizSummaryDto quizToSummary(Quiz quiz, ApplicationUser user) {
        int questionCount = questionRepository.countByQuiz_QuizId(quiz.getId());
        boolean areYouAuthor = quiz.getAuthor().equals(user);
        return quizMapper.toSummary(quiz, questionCount, areYouAuthor);
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
