package dev.six_seven_quiz.quiz.service;

import dev.six_seven_quiz.quiz.model.Question;
import dev.six_seven_quiz.quiz.model.QuestionSubmission;
import dev.six_seven_quiz.quiz.model.QuestionSubmissionId;
import dev.six_seven_quiz.quiz.model.QuizAttempt;
import dev.six_seven_quiz.quiz.repository.QuestionSubmissionRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class QuestionSubmissionService {

    private final QuestionSubmissionRepository questionSubmissionRepository;

    public QuestionSubmissionService(QuestionSubmissionRepository questionSubmissionRepository) {
        this.questionSubmissionRepository = questionSubmissionRepository;
    }

    public Optional<QuestionSubmission> findQuestionSubmission(QuizAttempt attempt, Question question) {
        return questionSubmissionRepository.findById(new QuestionSubmissionId(attempt.getId(), question.getId()));
    }
}
