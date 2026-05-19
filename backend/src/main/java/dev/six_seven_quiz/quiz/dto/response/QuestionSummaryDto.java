package dev.six_seven_quiz.quiz.dto.response;

import java.util.UUID;

public record QuestionSummaryDto(
        UUID id,
        String text
) {
}
