package dev.six_seven_quiz.quiz.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;

public record CreateQuizRequest(
        @NotNull
        @NotBlank
        String quizName,

        @NotNull
        Duration quizDuration
) {}
