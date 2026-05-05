package net.stachetopia.template.web.authentication.exception;

public class UsernameTooShortException extends RuntimeException {

    private final int minimumLength;

    public UsernameTooShortException(int minimumLength) {
        this.minimumLength = minimumLength;
        super("Password must be at least " + minimumLength + " characters long");
    }

    public int getMinimumLength() {
        return minimumLength;
    }
}
