package dev.six_seven_quiz.quiz.dto.response.viewing;

import java.time.Duration;
import java.util.UUID;

public record QuizSummaryDto(
    String name,
    int questionCount,
    Duration duration,
    UUID id,
    boolean youAreAuthor
) {
}
