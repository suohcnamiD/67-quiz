package dev.six_seven_quiz.quiz.dto.response;

import java.util.UUID;

public record OptionDto(
        UUID id,
        String text,
        boolean selected
) {
}
