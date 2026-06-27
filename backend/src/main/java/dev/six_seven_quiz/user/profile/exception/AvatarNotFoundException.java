package dev.six_seven_quiz.user.profile.exception;

public class AvatarNotFoundException extends RuntimeException {
    public AvatarNotFoundException(String username) {
        super("No avatar for " + username);
    }
}
