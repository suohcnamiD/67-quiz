package dev.six_seven_quiz.quiz.service;

import dev.six_seven_quiz.quiz.component.mapper.QuestionMapper;
import dev.six_seven_quiz.quiz.dto.QuestionData;
import dev.six_seven_quiz.quiz.dto.request.AddQuestionRequest;
import dev.six_seven_quiz.quiz.dto.request.DeleteQuestionRequest;
import dev.six_seven_quiz.quiz.dto.response.viewing.QuestionSummaryDto;
import dev.six_seven_quiz.quiz.exception.QuestionNotFoundException;
import dev.six_seven_quiz.quiz.exception.QuizNotFoundException;
import dev.six_seven_quiz.quiz.model.Question;
import dev.six_seven_quiz.quiz.model.Quiz;
import dev.six_seven_quiz.quiz.repository.OptionRepository;
import dev.six_seven_quiz.quiz.repository.QuestionRepository;
import dev.six_seven_quiz.quiz.repository.QuizRepository;
import dev.six_seven_quiz.quiz.validator.QuizValidator;
import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.ApplicationUserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
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

    @PersistenceContext
    private EntityManager entityManager;

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
        QuizValidator.requireOwner(quiz, user);

        quiz.addQuestion(new QuestionData(request.text(), request.options()));

        quiz = quizRepository.save(quiz);

        return quiz.getQuestions().stream().map(questionMapper::toSummaryDto).toList();
    }

    @Transactional
    public List<QuestionSummaryDto> deleteAsUser(UserDetails userDetails, @Valid DeleteQuestionRequest request) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        Question question = questionRepository.findById(request.questionId()).orElseThrow(() -> new QuestionNotFoundException(request.questionId()));
        Quiz quiz = question.getQuiz();
        QuizValidator.requireOwner(quiz, user);

        questionRepository.delete(question);

        entityManager.refresh(quiz);

        return quiz.getQuestions().stream().map(questionMapper::toSummaryDto).toList();
    }
}
