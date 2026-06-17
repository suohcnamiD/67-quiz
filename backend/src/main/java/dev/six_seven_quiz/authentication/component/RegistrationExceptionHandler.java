package dev.six_seven_quiz.authentication.component;

import dev.six_seven_quiz.authentication.AuthenticationController;
import dev.six_seven_quiz.authentication.exception.*;
import dev.six_seven_quiz.shared.dto.Failure;
import dev.six_seven_quiz.shared.dto.error.ApiError;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = AuthenticationController.class)
@Order(1)
public class RegistrationExceptionHandler {

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<Failure> handleDuplicateUsernameException(DuplicateUsernameException exception) {
        return Failure.of(HttpStatus.CONFLICT, ApiError.of("USERNAME_ALREADY_TAKEN")).toResponseEntity();
    }

    @ExceptionHandler(InvalidUsernameException.class)
    public ResponseEntity<Failure> handleInvalidUsernameException(InvalidUsernameException exception) {
        return Failure.of(HttpStatus.BAD_REQUEST, ApiError.of(
                "INVALID_USERNAME"
        )).toResponseEntity();
    }

    @ExceptionHandler(PasswordTooShortException.class)
    public ResponseEntity<Failure> handlePasswordTooShortException(PasswordTooShortException exception) {
        return Failure.of(HttpStatus.BAD_REQUEST, ApiError.of(
                "INVALID_PASSWORD",
                Map.of(
                        "violation", "TOO_SHORT",
                        "minimumLength", exception.getMinLength()
                    )
        )).toResponseEntity();
    }

    @ExceptionHandler(UsernameTooShortException.class)
    public ResponseEntity<Failure> handleUsernameTooShortException(UsernameTooShortException exception) {
        return Failure.of(HttpStatus.BAD_REQUEST, ApiError.of(
                "INVALID_USERNAME",
                Map.of(
                        "violation", "TOO_SHORT",
                        "minimumLength", exception.getMinimumLength()
                )
        )).toResponseEntity();
    }

    @ExceptionHandler(UsernameTooLongException.class)
    public ResponseEntity<Failure> handleUsernameTooLongException(UsernameTooLongException exception) {
        return Failure.of(HttpStatus.BAD_REQUEST, ApiError.of(
                "INVALID_USERNAME",
                Map.of(
                        "violation", "TOO_LONG",
                        "maximumLength", exception.getMaximumLength()
                )
        )).toResponseEntity();
    }
}
