package dev.six_seven_quiz.authentication.dto.request;

import jakarta.validation.constraints.NotNull;

public record RegistrationRequest(

        @NotNull
        String username,

        @NotNull
        String password
) {
}
