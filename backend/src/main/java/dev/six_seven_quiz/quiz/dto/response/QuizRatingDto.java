package dev.six_seven_quiz.quiz.dto.response;

import dev.six_seven_quiz.user.profile.dto.AuthorSummaryDto;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "A user's rating + optional comment on a quiz.")
@Relation(collectionRelation = "ratings", itemRelation = "rating")
public record QuizRatingDto(
        UUID id,
        int score,
        String comment,
        Instant createdAt,
        Instant updatedAt,
        AuthorSummaryDto author
) {
}
