package dev.six_seven_quiz.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "A page of leaderboard entries plus the signed-in user's overall rank (across the entire qualifying population), if they qualify.")
public record LeaderboardPageDto(
        List<LeaderboardEntryDto> entries,
        int page,
        int totalPages,
        long totalElements,
        LeaderboardEntryDto you
) {
}
