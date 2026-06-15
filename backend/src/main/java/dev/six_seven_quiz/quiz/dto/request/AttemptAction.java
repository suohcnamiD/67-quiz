package dev.six_seven_quiz.quiz.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttemptAction(

    @NotNull
    UUID questionId,
    @NotNull
    UUID optionId,
    @NotNull
    Boolean selected
) {
}
