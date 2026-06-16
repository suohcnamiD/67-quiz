package dev.six_seven_quiz.quiz.dto.response.attempt;

import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Relation(collectionRelation = "attempts", itemRelation = "attempt")
public record AttemptInProgressDto(
        UUID id,
        LocalDateTime startedAt,
        LocalDateTime finishDeadline,
        List<AttemptQuestionDto> questions
) {
}
