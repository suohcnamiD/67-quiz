package dev.six_seven_quiz.quiz.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeleteQuestionRequest(
        @NotNull
        UUID questionId
) {
}
