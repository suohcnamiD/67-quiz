package dev.six_seven_quiz.shared.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record ApiError(
        @Schema(description = "Stable, machine-readable error code. Switch on this on the client.",
                allowableValues = {
                        "QUIZ_NOT_FOUND",
                        "QUESTION_NOT_FOUND",
                        "OPTION_NOT_FOUND",
                        "ATTEMPT_NOT_FOUND",
                        "NO_ACCESS_TO_QUIZ",
                        "NO_ACCESS_TO_ATTEMPT",
                        "ATTEMPT_ALREADY_FINISHED",
                        "BLANK_OPTION_TEXT",
                        "INVALID_QUESTION_SHAPE",
                        "INVALID_REORDER",
                        "RATING_NOT_ELIGIBLE",
                        "INVALID_RATING",
                        "COMMENT_NOT_FOUND",
                        "NO_ACCESS_TO_COMMENT",
                        "INVALID_COMMENT",
                        "NOTIFICATION_NOT_FOUND",
                        "USERNAME_ALREADY_TAKEN",
                        "INVALID_USERNAME",
                        "INVALID_PASSWORD",
                        "USER_NOT_FOUND",
                        "INVALID_DISPLAY_NAME",
                        "INVALID_BIO",
                        "INVALID_IMAGE",
                        "AVATAR_TOO_LARGE",
                        "AVATAR_NOT_FOUND",
                        "VALIDATION_ERROR",
                        "INVALID_FORMAT",
                        "BAD_REQUEST",
                        "UNAUTHORIZED",
                        "FORBIDDEN",
                        "NOT_FOUND",
                        "METHOD_NOT_ALLOWED",
                        "UNSUPPORTED_MEDIA_TYPE",
                        "RATE_LIMITED",
                        "INTERNAL_SERVER_ERROR"
                })
        String code,
        @Schema(description = "Optional context for this error (e.g. offending field, rejected value, target id).")
        Map<String, Object> details
) {
    public static ApiError of(String code) {
        return new ApiError(code, Map.of());
    }

    public static ApiError of(String code, Map<String, Object> details) {
        return new ApiError(code, details);
    }
}
