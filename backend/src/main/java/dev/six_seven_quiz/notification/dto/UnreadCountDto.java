package dev.six_seven_quiz.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Unread notification count for the signed-in user.")
public record UnreadCountDto(long count) {}
