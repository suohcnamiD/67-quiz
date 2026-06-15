package dev.six_seven_quiz.quiz.exception;

import java.util.UUID;

public class NoAccessToQuizException extends RuntimeException {
    public UUID getQuizId() {
        return quizId;
    }

    private final UUID quizId;

    public NoAccessToQuizException(UUID quizId) {
        this.quizId = quizId;
    }
}
