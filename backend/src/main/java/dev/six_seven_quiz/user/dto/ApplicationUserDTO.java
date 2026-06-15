package dev.six_seven_quiz.user.dto;

import java.util.Set;
import java.util.UUID;

public record ApplicationUserDTO(
        UUID id,
        String username,
        Set<String> roles
) {
}
