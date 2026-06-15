package dev.six_seven_quiz.quiz.exception;

import java.util.UUID;

public class NoAccessToAttemptException extends RuntimeException {
    public UUID getAttemptId() {
        return attemptId;
    }

    private final UUID attemptId;

    public NoAccessToAttemptException(UUID attemptId) {
        this.attemptId = attemptId;
    }
}
