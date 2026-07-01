package dev.six_seven_quiz.quiz.dto.request;

import jakarta.validation.constraints.NotNull;

public record PinQuizRequest(
        @NotNull
        Boolean pinned
) {
}
