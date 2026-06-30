package dev.six_seven_quiz.notification.dto;

import dev.six_seven_quiz.notification.model.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Schema(description = "One notification line for the signed-in user.")
@Relation(collectionRelation = "notifications", itemRelation = "notification")
public record NotificationDto(
        UUID id,
        NotificationType type,
        Map<String, Object> payload,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
}
