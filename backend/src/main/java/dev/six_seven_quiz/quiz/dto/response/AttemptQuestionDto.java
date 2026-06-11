package dev.six_seven_quiz.quiz.dto.response;

import java.util.List;
import java.util.UUID;

public record AttemptQuestionDto(
        UUID id,
        List<OptionDto> options
) {
}
