package dev.six_seven_quiz.quiz.exception;

public class InvalidReorderException extends RuntimeException {
    private final String reason;

    public InvalidReorderException(String reason) {
        super(reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
