package dev.six_seven_quiz.quiz.exception;

public class InvalidQuestionShapeException extends RuntimeException {
    private final String reason;

    public InvalidQuestionShapeException(String reason) {
        super(reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
