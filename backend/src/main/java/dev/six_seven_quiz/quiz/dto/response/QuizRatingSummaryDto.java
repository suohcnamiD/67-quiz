package dev.six_seven_quiz.quiz.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Aggregate rating for a quiz: average score (1-10) and how many people rated.")
public record QuizRatingSummaryDto(
        Double average,
        long count
) {
}
