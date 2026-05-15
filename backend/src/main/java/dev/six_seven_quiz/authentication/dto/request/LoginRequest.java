package dev.six_seven_quiz.authentication.dto.request;

import jakarta.validation.constraints.NotNull;

public record LoginRequest(

        @NotNull
        String username,

        @NotNull
        String password
) {
}
