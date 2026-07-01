package dev.six_seven_quiz.quiz.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Body of PATCH /quiz/{id}/description. Null or empty clears the description.
 */
public record UpdateQuizDescriptionRequest(
        @Size(max = 10_000)
        String description
) {
}
