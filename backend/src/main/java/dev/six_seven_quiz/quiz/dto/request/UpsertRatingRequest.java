package dev.six_seven_quiz.quiz.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpsertRatingRequest(
        @Min(value = 1, message = "score must be at least 1")
        @Max(value = 10, message = "score must be at most 10")
        int score,
        @Size(max = 500, message = "comment must be at most 500 characters")
        String comment
) {
}
