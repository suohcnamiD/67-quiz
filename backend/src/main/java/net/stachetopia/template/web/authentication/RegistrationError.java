package net.stachetopia.template.web.authentication;

import net.stachetopia.template.web.shared.dto.error.ApiError;

import java.util.Map;

public interface RegistrationError {

    static ApiError passwordTooShort(int minimalLength) {
        return ApiError.of("PASSWORD_TOO_SHORT", Map.of("minimalLength", minimalLength));
    }

    static ApiError usernameTooShort(int minimalLength) {
        return ApiError.of("USERNAME_TOO_SHORT", Map.of("minimalLength", minimalLength));
    }
}
