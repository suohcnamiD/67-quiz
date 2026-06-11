package dev.six_seven_quiz.quiz.dto.request;

import java.util.UUID;

public record AttemptQuizRequest(
        UUID quizId
) {
}
