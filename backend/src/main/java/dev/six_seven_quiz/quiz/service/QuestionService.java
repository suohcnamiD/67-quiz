package dev.six_seven_quiz.quiz.service;

import dev.six_seven_quiz.quiz.component.mapper.QuestionMapper;
import dev.six_seven_quiz.quiz.dto.QuestionData;
import dev.six_seven_quiz.quiz.dto.request.AddQuestionRequest;
import dev.six_seven_quiz.quiz.dto.response.QuestionSummaryDto;
import dev.six_seven_quiz.quiz.exception.BlankIndexedOptionException;
import dev.six_seven_quiz.quiz.exception.NoAccessToQuizException;
import dev.six_seven_quiz.quiz.exception.QuizNotFoundException;
import dev.six_seven_quiz.quiz.model.Option;
import dev.six_seven_quiz.quiz.model.Question;
import dev.six_seven_quiz.quiz.model.Quiz;
import dev.six_seven_quiz.quiz.repository.OptionRepository;
import dev.six_seven_quiz.quiz.repository.QuestionRepository;
import dev.six_seven_quiz.quiz.repository.QuizRepository;
import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.ApplicationUserService;
import jakarta.transaction.Transactional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QuestionService {
    private final ApplicationUserService applicationUserService;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final QuestionMapper questionMapper;

    public QuestionService(ApplicationUserService applicationUserService, QuizRepository quizRepository, QuestionRepository questionRepository, OptionRepository optionRepository, QuestionMapper questionMapper) {
        this.applicationUserService = applicationUserService;
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.questionMapper = questionMapper;
    }

    @Transactional
    public List<QuestionSummaryDto> addQuizQuestionAsUser(
            UserDetails userDetails,
            AddQuestionRequest request
    ) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        Quiz quiz = quizRepository.findById(request.quizId()).orElseThrow(() -> new QuizNotFoundException(request.quizId()));
        if (!quiz.getAuthor().equals(user)) throw new NoAccessToQuizException(request.quizId());

        quiz.addQuestion(new QuestionData(request.text(), request.options()));

        quiz = quizRepository.save(quiz);

        return quiz.getQuestions().stream().map(questionMapper::toSummaryDto).toList();
    }
}
