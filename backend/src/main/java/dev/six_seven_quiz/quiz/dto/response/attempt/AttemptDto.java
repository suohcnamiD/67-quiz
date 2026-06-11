package dev.six_seven_quiz.quiz.dto.response.attempt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AttemptDto(
        UUID id,
        LocalDateTime startedAt,
        LocalDateTime finishDeadline,
        List<AttemptQuestionDto> questions
) {
}
