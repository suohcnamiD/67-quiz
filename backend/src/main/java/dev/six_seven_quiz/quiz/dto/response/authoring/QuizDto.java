package dev.six_seven_quiz.quiz.dto.response.authoring;

import dev.six_seven_quiz.quiz.dto.response.viewing.QuestionSummaryDto;
import org.springframework.hateoas.server.core.Relation;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Relation(collectionRelation = "quizzes", itemRelation = "quiz")
public record QuizDto(
    String name,
    int questionCount,
    Duration duration,
    UUID id,
    List<QuestionDto> questions
) {
}
