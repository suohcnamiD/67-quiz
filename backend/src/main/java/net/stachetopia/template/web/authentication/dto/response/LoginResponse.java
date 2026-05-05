package net.stachetopia.template.web.authentication.dto.response;

import java.util.List;

public record LoginResponse(
        List<String> roles
) {
}
