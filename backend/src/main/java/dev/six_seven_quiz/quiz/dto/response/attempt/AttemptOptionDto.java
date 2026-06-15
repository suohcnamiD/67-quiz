package dev.six_seven_quiz.quiz.dto.response.attempt;

import java.util.UUID;

public record AttemptOptionDto(
        UUID id,
        String text,
        boolean selected
) {
}
