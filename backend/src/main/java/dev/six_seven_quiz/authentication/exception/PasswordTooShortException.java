package dev.six_seven_quiz.authentication.exception;

public class PasswordTooShortException extends RuntimeException {

    private final int minLength;

    public PasswordTooShortException(int minLength) {
        this.minLength = minLength;
        super("Password must be at least " + minLength + " characters long");
    }

    public int getMinLength() {
        return minLength;
    }
}
