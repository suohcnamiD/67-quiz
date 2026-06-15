package dev.six_seven_quiz.authentication.exception;

public class UsernameTooLongException extends RuntimeException {

    private final int maximumLength;

    public UsernameTooLongException(int maximumLength) {
        this.maximumLength = maximumLength;
        super("Username must be at most " + maximumLength + " characters long");
    }

    public int getMaximumLength() {
        return maximumLength;
    }
}
