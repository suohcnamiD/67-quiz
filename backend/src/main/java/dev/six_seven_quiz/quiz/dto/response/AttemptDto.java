package dev.six_seven_quiz.quiz.dto.response;

import dev.six_seven_quiz.quiz.model.AttemptQuestion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AttemptDto(
        UUID id,
        LocalDateTime startedAt,
        List<AttemptQuestionDto> questions
) {
}
