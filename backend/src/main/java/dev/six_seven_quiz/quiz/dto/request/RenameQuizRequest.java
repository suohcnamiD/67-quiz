package dev.six_seven_quiz.quiz.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RenameQuizRequest(
        @NotNull
        @NotBlank
        @Size(max = 200)
        String name
) {
}
