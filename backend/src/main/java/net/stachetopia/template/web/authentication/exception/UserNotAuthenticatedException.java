package net.stachetopia.template.web.authentication.exception;

public class UserNotAuthenticatedException extends RuntimeException {
    public UserNotAuthenticatedException() {
        super("User is not authenticated");
    }
}
