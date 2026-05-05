package net.stachetopia.template.web.shared.dto.error;

import java.util.Map;

public record ApiError(
        String code,
        Map<String, Object> details
) {
    public static ApiError of(String code) {
        return new ApiError(code, Map.of());
    }

    public static ApiError of(String code, Map<String, Object> details) {
        return new ApiError(code, details);
    }
}
