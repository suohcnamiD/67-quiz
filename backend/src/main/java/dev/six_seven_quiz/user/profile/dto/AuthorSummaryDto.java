package dev.six_seven_quiz.user.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Minimal author projection — username and display name, enough to link to a profile page.")
public record AuthorSummaryDto(
        String username,
        String displayName,
        boolean hasAvatar
) {
}
