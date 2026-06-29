package dev.six_seven_quiz.user.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "A comment on a user's profile.")
@Relation(collectionRelation = "comments", itemRelation = "comment")
public record ProfileCommentDto(
        UUID id,
        String body,
        Instant createdAt,
        AuthorSummaryDto author,
        boolean canDelete
) {
}
