package dev.six_seven_quiz.quiz.service;

import dev.six_seven_quiz.quiz.component.mapper.QuizMapper;
import dev.six_seven_quiz.quiz.dto.request.CreateQuizRequest;
import dev.six_seven_quiz.quiz.dto.response.AttemptDto;
import dev.six_seven_quiz.quiz.dto.response.QuizDto;
import dev.six_seven_quiz.quiz.exception.QuizNotFoundException;
import dev.six_seven_quiz.quiz.model.Quiz;
import dev.six_seven_quiz.quiz.model.QuizAttempt;
import dev.six_seven_quiz.quiz.repository.QuizAttemptRepository;
import dev.six_seven_quiz.quiz.repository.QuizRepository;
import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.ApplicationUserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class QuizService {
    private final ApplicationUserService applicationUserService;
    private final QuizRepository quizRepository;
    private final QuizMapper quizMapper;
    private final QuizAttemptRepository quizAttemptRepository;

    public QuizService(ApplicationUserService applicationUserService, QuizRepository quizRepository, QuizMapper quizMapper, QuizAttemptRepository quizAttemptRepository) {
        this.applicationUserService = applicationUserService;
        this.quizRepository = quizRepository;
        this.quizMapper = quizMapper;
        this.quizAttemptRepository = quizAttemptRepository;
    }

    public QuizDto createQuiz(UserDetails userDetails, CreateQuizRequest request) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);

        Quiz rawNewQuiz = new Quiz(request.quizName(), user);
        rawNewQuiz.setAuthor(user);

        Quiz newQuiz = quizRepository.save(rawNewQuiz);
        return quizMapper.toDto(newQuiz);
    }
}
