package dev.six_seven_quiz.quiz.exception;

import java.util.UUID;

public class AttemptNotFoundException extends RuntimeException {
    public UUID getAttemptId() {
        return attemptId;
    }

    private final UUID attemptId;

    public AttemptNotFoundException(UUID attemptId) {
        super(String.format("Attempt with id %s not found", attemptId));
        this.attemptId = attemptId;
    }
}
