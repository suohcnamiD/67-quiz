package dev.six_seven_quiz.quiz.exception;

import java.util.UUID;

public class RatingNotEligibleException extends RuntimeException {
    private final UUID quizId;

    public RatingNotEligibleException(UUID quizId) {
        super(String.format("Cannot rate quiz %s without a finished attempt", quizId));
        this.quizId = quizId;
    }

    public UUID getQuizId() {
        return quizId;
    }
}
