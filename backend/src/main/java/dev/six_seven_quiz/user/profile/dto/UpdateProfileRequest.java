package dev.six_seven_quiz.user.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Patch payload for the caller's own profile. Any field left null is left unchanged.")
public record UpdateProfileRequest(
        @Schema(description = "New display name. 1–32 characters after trimming.")
        @Size(min = 1, max = 32) String displayName,
        @Schema(description = "Bio / short about. Up to 280 characters. Empty string clears it.")
        @Size(max = 280) String bio
) {
}
