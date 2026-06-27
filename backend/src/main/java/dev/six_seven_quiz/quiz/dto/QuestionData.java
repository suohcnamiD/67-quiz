package dev.six_seven_quiz.quiz.dto;

import dev.six_seven_quiz.quiz.dto.request.OptionData;
import dev.six_seven_quiz.quiz.model.QuestionType;

import java.util.List;

public record QuestionData(
        String text,
        QuestionType type,
        List<OptionData> options
) {
}
