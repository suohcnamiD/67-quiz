package net.stachetopia.template.web.shared.dto;

import net.stachetopia.template.web.shared.dto.error.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public record Failure(
        HttpStatus status,
        List<ApiError> errors
) {
    public static Failure of(HttpStatus status, ApiError error) {
        return new Failure(status, List.of(error));
    }

    public static Failure of(HttpStatus status, List<ApiError> errors) {
        return new Failure(status, errors);
    }

    public static Failure status(HttpStatus status) {
        return of(status, ApiError.of(status.name()));
    }

    public ResponseEntity<Failure> toResponseEntity() {
        return ResponseEntity.status(status).body(this);
    }

    public static Failure invalidFormat(String fieldName) {
        return of(HttpStatus.BAD_REQUEST, ApiError.of("INVALID_FORMAT", Map.of("field", fieldName)));
    };
}
