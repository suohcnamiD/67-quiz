package dev.six_seven_quiz.quiz.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateQuizRequest(
        @NotNull
        @NotBlank
        String quizName
) {}
