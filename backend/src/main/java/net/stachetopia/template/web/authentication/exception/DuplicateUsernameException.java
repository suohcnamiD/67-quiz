package net.stachetopia.template.web.authentication.exception;

public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException() {
        super("Username is already taken");
    }
}
