package dev.six_seven_quiz.quiz.dto.response.viewing;

import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Finished attempt
@Relation(collectionRelation = "attempts", itemRelation = "attempt")
public record FinishedAttemptSummaryDto(
        UUID id,
        QuizSummaryDto quiz,
        Instant startedAt,
        Instant finishDeadline,
        List<FinishedQuestionDto> questions,
        int maximumScore,
        int score
) {
}
