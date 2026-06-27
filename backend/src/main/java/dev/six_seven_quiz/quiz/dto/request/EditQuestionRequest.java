package dev.six_seven_quiz.quiz.dto.request;

import dev.six_seven_quiz.quiz.model.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record EditQuestionRequest(
        @NotBlank
        @NotNull
        String text,

        @NotNull
        QuestionType type,

        @NotNull
        List<OptionData> options
) {
}
