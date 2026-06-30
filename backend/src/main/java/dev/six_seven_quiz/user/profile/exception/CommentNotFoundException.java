package dev.six_seven_quiz.user.profile.exception;

import java.util.UUID;

public class CommentNotFoundException extends RuntimeException {
    private final UUID commentId;

    public CommentNotFoundException(UUID commentId) {
        super(String.format("Comment %s not found", commentId));
        this.commentId = commentId;
    }

    public UUID getCommentId() {
        return commentId;
    }
}
