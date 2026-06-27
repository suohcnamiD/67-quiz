package dev.six_seven_quiz.quiz.dto.request;

import dev.six_seven_quiz.quiz.model.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AddQuestionRequest(
        @NotNull
        UUID quizId,

        @NotBlank
        @NotNull
        String text,

        @NotNull
        QuestionType type,

        @NotNull
        List<OptionData> options
) {
}
