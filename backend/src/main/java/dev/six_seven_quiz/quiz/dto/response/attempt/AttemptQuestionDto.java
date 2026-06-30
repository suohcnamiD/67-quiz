package dev.six_seven_quiz.quiz.dto.response.attempt;

import dev.six_seven_quiz.quiz.model.QuestionType;
import org.springframework.hateoas.server.core.Relation;

import java.util.List;
import java.util.UUID;

@Relation(collectionRelation = "questions", itemRelation = "question")
public record AttemptQuestionDto(
        UUID id,
        UUID questionId,
        String text,
        QuestionType type,
        boolean hasImage,
        List<AttemptOptionDto> options
) {
}
