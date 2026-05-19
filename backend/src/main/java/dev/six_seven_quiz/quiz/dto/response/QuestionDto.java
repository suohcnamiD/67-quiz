package dev.six_seven_quiz.quiz.dto.response;

import java.util.List;
import java.util.UUID;

public record QuestionDto(
        UUID id,
        String text,
        List<OptionDto> options
) {
}
