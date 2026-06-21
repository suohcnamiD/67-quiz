package dev.six_seven_quiz.user.profile.exception;

public class AvatarTooLargeException extends RuntimeException {
    private final long maxBytes;

    public AvatarTooLargeException(long maxBytes) {
        super("Avatar exceeds " + maxBytes + " bytes");
        this.maxBytes = maxBytes;
    }

    public long getMaxBytes() {
        return maxBytes;
    }
}
