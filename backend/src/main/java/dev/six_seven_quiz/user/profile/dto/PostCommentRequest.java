package dev.six_seven_quiz.user.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostCommentRequest(
        @NotBlank(message = "body must not be empty")
        @Size(max = 1000, message = "body must be at most 1000 characters")
        String body
) {
}
