package dev.six_seven_quiz.search.dto;

import dev.six_seven_quiz.quiz.dto.response.viewing.QuizSummaryDto;
import dev.six_seven_quiz.user.profile.dto.AuthorSummaryDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Unified search results — matched quizzes by name and matched users by username/displayName.")
public record SearchResponseDto(
        @Schema(description = "Matched quizzes, capped at the per-side page size.")
        List<QuizSummaryDto> quizzes,
        @Schema(description = "Matched users, capped at the per-side page size.")
        List<AuthorSummaryDto> users,
        @Schema(description = "Echo of the trimmed query string the server actually searched for.")
        String query
) {
}
