package dev.six_seven_quiz.quiz.dto.response.viewing;

import java.util.UUID;

public record QuestionSummaryDto(
        UUID id,
        String text
) {
}
