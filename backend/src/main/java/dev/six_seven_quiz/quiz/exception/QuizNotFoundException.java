package dev.six_seven_quiz.quiz.exception;

import java.util.UUID;

public class QuizNotFoundException extends RuntimeException {
    public UUID getQuizId() {
        return quizId;
    }

    private final UUID quizId;

    public QuizNotFoundException(UUID quizId) {
        super(String.format("Quiz with id %s not found", quizId));
        this.quizId = quizId;
    }
}
