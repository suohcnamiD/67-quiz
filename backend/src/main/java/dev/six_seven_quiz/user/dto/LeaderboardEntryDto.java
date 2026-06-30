package dev.six_seven_quiz.user.dto;

import dev.six_seven_quiz.user.profile.dto.AuthorSummaryDto;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

@Schema(description = "One row on a leaderboard. Primary value is the headline metric (avg score % or avg rating), secondary is the volume signal (attempt count or rating count).")
@Relation(collectionRelation = "entries", itemRelation = "entry")
public record LeaderboardEntryDto(
        int rank,
        AuthorSummaryDto user,
        double primaryValue,
        long secondaryValue
) {
}
