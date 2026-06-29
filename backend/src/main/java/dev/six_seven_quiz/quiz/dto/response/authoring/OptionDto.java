package dev.six_seven_quiz.quiz.dto.response.authoring;

import org.springframework.hateoas.server.core.Relation;

import java.util.UUID;

@Relation(collectionRelation = "options", itemRelation = "option")
public record OptionDto(
        UUID id,
        String text,
        boolean correct,
        boolean hasImage
) {
}
