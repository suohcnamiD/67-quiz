package dev.six_seven_quiz.user.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Public-facing user profile. Editable fields are mutable only on /users/me.")
public record UserProfileDto(
        String username,
        String displayName,
        String bio,
        boolean hasAvatar,
        int quizzesAuthored,
        int attemptsTaken,
        @Schema(description = "Mean score across finished attempts as a percentage (0–100). Null if the user has no finished attempts yet.")
        Integer averageScorePercent,
        @Schema(description = "True when this profile belongs to the currently authenticated caller. Lets the frontend show edit affordances without a second request.")
        boolean isYou
) {
}
