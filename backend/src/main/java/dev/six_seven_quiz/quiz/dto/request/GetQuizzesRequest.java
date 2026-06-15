package dev.six_seven_quiz.quiz.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GetQuizzesRequest(

        @NotNull
        @NotBlank
        UUID quizId
) {
}
