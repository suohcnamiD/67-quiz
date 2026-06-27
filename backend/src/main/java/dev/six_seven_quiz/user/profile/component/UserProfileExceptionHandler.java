package dev.six_seven_quiz.user.profile.component;

import dev.six_seven_quiz.shared.dto.Failure;
import dev.six_seven_quiz.shared.dto.error.ApiError;
import dev.six_seven_quiz.user.profile.controller.UserProfileController;
import dev.six_seven_quiz.user.profile.exception.AvatarNotFoundException;
import dev.six_seven_quiz.user.profile.exception.AvatarTooLargeException;
import dev.six_seven_quiz.user.profile.exception.InvalidDisplayNameException;
import dev.six_seven_quiz.user.profile.exception.InvalidImageException;
import dev.six_seven_quiz.user.profile.exception.UnknownUsernameException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice(assignableTypes = UserProfileController.class)
@Order(1)
public class UserProfileExceptionHandler {

    @ExceptionHandler(UnknownUsernameException.class)
    public ResponseEntity<Failure> handleUnknownUsername(UnknownUsernameException exception) {
        return Failure.of(
                HttpStatus.NOT_FOUND,
                ApiError.of("USER_NOT_FOUND", Map.of("username", exception.getUsername()))
        ).toResponseEntity();
    }

    @ExceptionHandler(AvatarNotFoundException.class)
    public ResponseEntity<Failure> handleAvatarNotFound(AvatarNotFoundException exception) {
        return Failure.of(HttpStatus.NOT_FOUND, ApiError.of("AVATAR_NOT_FOUND")).toResponseEntity();
    }

    @ExceptionHandler(InvalidDisplayNameException.class)
    public ResponseEntity<Failure> handleInvalidDisplayName(InvalidDisplayNameException exception) {
        // Reuse INVALID_DISPLAY_NAME for both displayName + bio length errors. The message
        // carries the reason; the frontend just shows the friendly fallback for now.
        return Failure.of(
                HttpStatus.BAD_REQUEST,
                ApiError.of("INVALID_DISPLAY_NAME", Map.of("message", exception.getMessage()))
        ).toResponseEntity();
    }

    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<Failure> handleInvalidImage(InvalidImageException exception) {
        return Failure.of(
                HttpStatus.BAD_REQUEST,
                ApiError.of("INVALID_IMAGE", Map.of("message", exception.getMessage()))
        ).toResponseEntity();
    }

    @ExceptionHandler(AvatarTooLargeException.class)
    public ResponseEntity<Failure> handleAvatarTooLarge(AvatarTooLargeException exception) {
        return Failure.of(
                HttpStatus.PAYLOAD_TOO_LARGE,
                ApiError.of("AVATAR_TOO_LARGE", Map.of("maxBytes", exception.getMaxBytes()))
        ).toResponseEntity();
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Failure> handleSpringMaxUpload(MaxUploadSizeExceededException exception) {
        // Spring's filter catches over-cap uploads before our service sees them.
        return Failure.of(
                HttpStatus.PAYLOAD_TOO_LARGE,
                ApiError.of("AVATAR_TOO_LARGE", Map.of("maxBytes", exception.getMaxUploadSize()))
        ).toResponseEntity();
    }
}
