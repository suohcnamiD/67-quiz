package dev.six_seven_quiz.quiz.dto.response.authoring;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public record QuizDto(
    String name,
    int questionCount,
    Duration duration,
    UUID id,
    List<QuestionDto> questions
) {
}
