package dev.six_seven_quiz.user.profile.component;

import dev.six_seven_quiz.shared.dto.Failure;
import dev.six_seven_quiz.shared.dto.error.ApiError;
import dev.six_seven_quiz.user.profile.controller.ProfileCommentController;
import dev.six_seven_quiz.user.profile.exception.CommentNotFoundException;
import dev.six_seven_quiz.user.profile.exception.InvalidCommentException;
import dev.six_seven_quiz.user.profile.exception.NoAccessToCommentException;
import dev.six_seven_quiz.user.profile.exception.UnknownUsernameException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = ProfileCommentController.class)
@Order(1)
public class ProfileCommentControllerExceptionHandler {

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<Failure> handleNotFound(CommentNotFoundException ex) {
        return Failure.of(HttpStatus.NOT_FOUND, ApiError.of("COMMENT_NOT_FOUND", Map.of("id", ex.getCommentId()))).toResponseEntity();
    }

    @ExceptionHandler(NoAccessToCommentException.class)
    public ResponseEntity<Failure> handleNoAccess(NoAccessToCommentException ex) {
        return Failure.of(HttpStatus.FORBIDDEN, ApiError.of("NO_ACCESS_TO_COMMENT", Map.of("id", ex.getCommentId()))).toResponseEntity();
    }

    @ExceptionHandler(InvalidCommentException.class)
    public ResponseEntity<Failure> handleInvalid(InvalidCommentException ex) {
        return Failure.of(HttpStatus.BAD_REQUEST, ApiError.of("INVALID_COMMENT", Map.of("message", ex.getMessage()))).toResponseEntity();
    }

    @ExceptionHandler(UnknownUsernameException.class)
    public ResponseEntity<Failure> handleUnknownUser(UnknownUsernameException ex) {
        return Failure.of(HttpStatus.NOT_FOUND, ApiError.of("USER_NOT_FOUND", Map.of("username", ex.getUsername()))).toResponseEntity();
    }
}
