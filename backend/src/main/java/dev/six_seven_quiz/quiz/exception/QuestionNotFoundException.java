package dev.six_seven_quiz.quiz.exception;

import java.util.UUID;

public class QuestionNotFoundException extends RuntimeException {

    public UUID getQuestionId() {
        return questionId;
    }

    private final UUID questionId;

    public QuestionNotFoundException(UUID questionId) {
        super("Question with ID " + questionId + " not found.");
        this.questionId = questionId;
    }
}
