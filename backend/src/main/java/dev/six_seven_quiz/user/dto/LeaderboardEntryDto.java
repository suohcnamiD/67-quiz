package dev.six_seven_quiz.user.dto;

import dev.six_seven_quiz.user.profile.dto.AuthorSummaryDto;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

@Schema(description = """
        One row on a leaderboard.
        primaryValue is the headline metric used for ranking (adjusted score for players, avg rating for authors).
        secondaryValue is the volume signal (attempt count for players, rating count for authors).
        tertiaryValue is an optional raw input — for the players board it's the user's true average accuracy %, surfaced
        so the FE can explain why a 80%/many-attempts player can sit above a 90%/few-attempts one. Null on the authors
        board.
        """)
@Relation(collectionRelation = "entries", itemRelation = "entry")
public record LeaderboardEntryDto(
        int rank,
        AuthorSummaryDto user,
        double primaryValue,
        long secondaryValue,
        Double tertiaryValue
) {
}
