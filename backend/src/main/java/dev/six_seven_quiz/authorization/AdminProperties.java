package dev.six_seven_quiz.authorization;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Comma-separated list of usernames to promote to ADMIN on startup. Wire
 * it via APP_ADMIN_USERNAMES=alice,bob in prod compose or
 * app.admin.usernames in application.yaml locally.
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(List<String> usernames) {
    public AdminProperties {
        if (usernames == null) usernames = List.of();
    }
}
