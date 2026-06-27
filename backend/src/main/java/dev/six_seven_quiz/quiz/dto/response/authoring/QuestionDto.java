package dev.six_seven_quiz.quiz.dto.response.authoring;

import dev.six_seven_quiz.quiz.model.QuestionType;
import org.springframework.hateoas.server.core.Relation;

import java.util.List;
import java.util.UUID;

@Relation(collectionRelation = "questions", itemRelation = "question")
public record QuestionDto(
        UUID id,
        String text,
        QuestionType type,
        List<OptionDto> options
) {
}
