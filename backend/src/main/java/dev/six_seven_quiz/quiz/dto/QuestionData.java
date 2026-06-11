package dev.six_seven_quiz.quiz.dto;

import dev.six_seven_quiz.quiz.dto.request.OptionData;

import java.util.List;

public record QuestionData(
        String text,
        List<OptionData> options
) {
}
