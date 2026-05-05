package net.stachetopia.template.web.authentication.dto.request;

import jakarta.validation.constraints.NotNull;

public record RegistrationRequest(

        @NotNull
        String username,

        @NotNull
        String password
) {
}
