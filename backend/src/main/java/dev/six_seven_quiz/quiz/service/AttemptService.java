package dev.six_seven_quiz.quiz.service;

import dev.six_seven_quiz.quiz.component.mapper.*;
import dev.six_seven_quiz.quiz.dto.request.AttemptAction;
import dev.six_seven_quiz.quiz.dto.request.CommitAttemptActionsRequest;
import dev.six_seven_quiz.quiz.dto.response.AttemptDto;
import dev.six_seven_quiz.quiz.dto.response.AttemptQuestionDto;
import dev.six_seven_quiz.quiz.dto.response.OptionDto;
import dev.six_seven_quiz.quiz.exception.AttemptNotFoundException;
import dev.six_seven_quiz.quiz.exception.NoAccessToAttemptException;
import dev.six_seven_quiz.quiz.exception.QuizNotFoundException;
import dev.six_seven_quiz.quiz.model.*;
import dev.six_seven_quiz.quiz.repository.QuizAttemptRepository;
import dev.six_seven_quiz.quiz.repository.QuizRepository;
import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.ApplicationUserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

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

    @PersistenceContext
    private EntityManager entityManager;

    public AttemptService(ApplicationUserService applicationUserService, QuizAttemptRepository quizAttemptRepository, QuizRepository quizRepository, AttemptMapper attemptMapper, OptionMapper optionMapper, AttemptQuestionMapper attemptQuestionMapper) {
        this.applicationUserService = applicationUserService;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizRepository = quizRepository;
        this.attemptMapper = attemptMapper;
        this.optionMapper = optionMapper;
        this.attemptQuestionMapper = attemptQuestionMapper;
    }

    @Transactional
    public AttemptDto commitAttemptActionsAsUser(UserDetails userDetails, CommitAttemptActionsRequest request) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        Attempt attempt = quizAttemptRepository.findById(request.attemptId()).orElseThrow(() -> new AttemptNotFoundException(request.attemptId()));
        validateUserAttemptOwnership(user, attempt);
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
    public AttemptDto attemptQuizAsUser(UserDetails userDetails, UUID quizId) {
        ApplicationUser user = applicationUserService.getAuthenticatedUserFromDetails(userDetails);
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new QuizNotFoundException(quizId));

        Attempt attempt = quiz.start(user);
        attempt = quizAttemptRepository.save(attempt);
        entityManager.flush();
        entityManager.refresh(attempt);
        return attemptToDto(attempt);

    }

    private void validateUserAttemptOwnership(ApplicationUser user, Attempt attempt) {
        if (!attempt.getUser().equals(user)) throw new NoAccessToAttemptException(attempt.getId());
    }

    private AttemptDto attemptToDto(Attempt attempt) {
        List<AttemptQuestionDto> attemptQuestions = attempt.getQuestions().stream().map(question -> {
            List<Option> generalOptions = question.getOptions();
            List<Option> selectedOptions = question.getSelectedOptions();

            List<OptionDto> optionDtos = new ArrayList<>();

            for (Option option : generalOptions) {
                boolean selected = selectedOptions.contains(option);
                optionDtos.add(optionMapper.toDto(option, selected));
            }
            return attemptQuestionMapper.toDto(question, optionDtos);
        }).toList();
        return attemptMapper.toDto(attempt, attemptQuestions);
    }
}
