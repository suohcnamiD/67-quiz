package dev.six_seven_quiz.user.profile.exception;

import java.util.UUID;

public class NoAccessToCommentException extends RuntimeException {
    private final UUID commentId;

    public NoAccessToCommentException(UUID commentId) {
        super(String.format("No access to comment %s", commentId));
        this.commentId = commentId;
    }

    public UUID getCommentId() {
        return commentId;
    }
}
