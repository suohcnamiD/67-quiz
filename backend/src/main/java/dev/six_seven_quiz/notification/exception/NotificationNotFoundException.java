package dev.six_seven_quiz.notification.exception;

import java.util.UUID;

public class NotificationNotFoundException extends RuntimeException {
    private final UUID id;

    public NotificationNotFoundException(UUID id) {
        super("Notification " + id + " not found");
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
