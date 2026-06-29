package dev.six_seven_quiz.quiz.dto.response.attempt;

import org.springframework.hateoas.server.core.Relation;

import java.util.UUID;

@Relation(collectionRelation = "options", itemRelation = "option")
public record AttemptOptionDto(
        UUID id,
        String text,
        boolean selected,
        boolean hasImage
) {
}
