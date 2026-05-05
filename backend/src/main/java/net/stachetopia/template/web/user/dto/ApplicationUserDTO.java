package net.stachetopia.template.web.user.dto;

import java.util.Set;
import java.util.UUID;

public record ApplicationUserDTO(
        UUID id,
        String username,
        Set<String> roles
) {
}
