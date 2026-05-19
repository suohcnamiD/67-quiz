package dev.six_seven_quiz.quiz.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AddQuestionRequest(
        @NotNull
        UUID quizId,

        @NotBlank
        @NotNull
        String text,

        @NotNull
        Map<String, Boolean> options
) {
}
