package dev.six_seven_quiz.quiz.service;

import dev.six_seven_quiz.quiz.QuizMapper;
import dev.six_seven_quiz.quiz.dto.request.CreateQuizRequest;
import dev.six_seven_quiz.quiz.dto.response.QuizDto;
import dev.six_seven_quiz.quiz.model.Quiz;
import dev.six_seven_quiz.quiz.repository.QuizRepository;
import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.ApplicationUserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class QuizService {
    private final ApplicationUserService applicationUserService;
    private final QuizRepository quizRepository;
    private final QuizMapper quizMapper;

    public QuizService(ApplicationUserService applicationUserService, QuizRepository quizRepository, QuizMapper quizMapper) {
        this.applicationUserService = applicationUserService;
        this.quizRepository = quizRepository;
        this.quizMapper = quizMapper;
    }

    public QuizDto createQuiz(UserDetails userDetails, CreateQuizRequest request) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);

        Quiz rawNewQuiz = new Quiz(request.quizName());
        rawNewQuiz.setAuthor(user);

        Quiz newQuiz = quizRepository.save(rawNewQuiz);
        return quizMapper.toDto(newQuiz);
    }
}
