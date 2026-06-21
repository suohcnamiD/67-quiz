package dev.six_seven_quiz.user.profile.exception;

public class UnknownUsernameException extends RuntimeException {
    private final String username;

    public UnknownUsernameException(String username) {
        super("No user with username " + username);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
