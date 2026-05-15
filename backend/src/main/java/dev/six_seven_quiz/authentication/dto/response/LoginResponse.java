package dev.six_seven_quiz.authentication.dto.response;

import java.util.List;

public record LoginResponse(
        List<String> roles
) {
}
