package dev.six_seven_quiz.quiz.dto.response.viewing;

import org.springframework.hateoas.server.core.Relation;

import java.util.UUID;

@Relation(collectionRelation = "options", itemRelation = "option")
public record FinishedOptionDto(
        UUID id,
        String text,
        boolean correct,
        boolean selected,
        boolean hasImage
) {
    public boolean isCorrectlySelected() {
        return correct == selected;
    }
}
