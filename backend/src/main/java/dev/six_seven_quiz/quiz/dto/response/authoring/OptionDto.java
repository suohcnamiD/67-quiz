package dev.six_seven_quiz.quiz.dto.response.authoring;

import java.util.UUID;

public record OptionDto(
        UUID id,
        String text,
        boolean correct
) {
}
