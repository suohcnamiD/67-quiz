package dev.six_seven_quiz.quiz.service;

import dev.six_seven_quiz.quiz.component.mapper.AttemptMapper;
import dev.six_seven_quiz.quiz.component.mapper.OptionMapper;
import dev.six_seven_quiz.quiz.component.mapper.QuestionMapper;
import dev.six_seven_quiz.quiz.component.mapper.QuizMapper;
import dev.six_seven_quiz.quiz.dto.response.AttemptDto;
import dev.six_seven_quiz.quiz.dto.response.OptionDto;
import dev.six_seven_quiz.quiz.dto.response.QuestionDto;
import dev.six_seven_quiz.quiz.exception.QuizNotFoundException;
import dev.six_seven_quiz.quiz.model.*;
import dev.six_seven_quiz.quiz.repository.QuestionSubmissionRepository;
import dev.six_seven_quiz.quiz.repository.QuizAttemptRepository;
import dev.six_seven_quiz.quiz.repository.QuizRepository;
import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.ApplicationUserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AttemptService {
    private final ApplicationUserService applicationUserService;
    private final QuizService quizService;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizMapper quizMapper;
    private final QuizRepository quizRepository;
    private final AttemptMapper attemptMapper;
    private final QuestionService questionService;
    private final QuestionSubmissionRepository questionSubmissionRepository;
    private final QuestionSubmissionService questionSubmissionService;
    private final QuestionMapper questionMapper;
    private final OptionMapper optionMapper;

    public AttemptService(ApplicationUserService applicationUserService, QuizService quizService, QuizAttemptRepository quizAttemptRepository, QuizMapper quizMapper, QuizRepository quizRepository, AttemptMapper attemptMapper, QuestionService questionService, QuestionSubmissionRepository questionSubmissionRepository, QuestionSubmissionService questionSubmissionService, QuestionMapper questionMapper, OptionMapper optionMapper) {
        this.applicationUserService = applicationUserService;
        this.quizService = quizService;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizMapper = quizMapper;
        this.quizRepository = quizRepository;
        this.attemptMapper = attemptMapper;
        this.questionService = questionService;
        this.questionSubmissionRepository = questionSubmissionRepository;
        this.questionSubmissionService = questionSubmissionService;
        this.questionMapper = questionMapper;
        this.optionMapper = optionMapper;
    }

    public AttemptDto attemptQuizAsUser(UserDetails userDetails, UUID quizId) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new QuizNotFoundException(quizId));

        QuizAttempt attempt = new QuizAttempt(user, quiz);
        attempt = quizAttemptRepository.save(attempt);

        List<QuestionDto> questionDtos = getAttemptQuestions(attempt);
        return attemptMapper.toDto(attempt, questionDtos);
    }

    private List<QuestionDto> getAttemptQuestions(QuizAttempt attempt) {
        Quiz quiz = attempt.getQuiz();
        List<QuestionDto> result = new ArrayList<>();
        List<Question> quizQuestions = questionService.getQuizQuestions(quiz);

        for (Question question : quizQuestions) {
            Optional<QuestionSubmission> existingSubmissionOptional = questionSubmissionService.findQuestionSubmission(attempt, question);
            List<OptionDto> optionDtos;
            if (existingSubmissionOptional.isEmpty()) {
                optionDtos = question.getOptions().stream().map(option -> optionMapper.toDto(option, false)).toList();
            } else {
                QuestionSubmission existingSubmission = existingSubmissionOptional.get();
                optionDtos = question.getOptions().stream().map(option -> new OptionDto(option.getId(), option.getText(), existingSubmission.getSelectedOptions().contains(option))).toList();
            }
            result.add(questionMapper.toDto(question, optionDtos));
        }

        return result;
    }
}
