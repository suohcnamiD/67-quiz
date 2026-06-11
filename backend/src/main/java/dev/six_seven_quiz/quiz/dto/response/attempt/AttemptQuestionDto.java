package dev.six_seven_quiz.quiz.dto.response.attempt;

import java.util.List;
import java.util.UUID;

public record AttemptQuestionDto(
        UUID id,
        String text,
        List<AttemptOptionDto> options
) {
}
