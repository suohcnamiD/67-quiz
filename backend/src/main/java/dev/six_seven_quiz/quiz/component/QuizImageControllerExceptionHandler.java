package dev.six_seven_quiz.quiz.component;

import dev.six_seven_quiz.quiz.controller.QuizImageController;
import dev.six_seven_quiz.quiz.exception.NoAccessToQuizException;
import dev.six_seven_quiz.quiz.exception.OptionNotFoundException;
import dev.six_seven_quiz.quiz.exception.QuestionNotFoundException;
import dev.six_seven_quiz.quiz.exception.QuizNotFoundException;
import dev.six_seven_quiz.shared.dto.Failure;
import dev.six_seven_quiz.shared.dto.error.ApiError;
import dev.six_seven_quiz.user.profile.exception.AvatarTooLargeException;
import dev.six_seven_quiz.user.profile.exception.InvalidImageException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice(assignableTypes = QuizImageController.class)
@Order(1)
public class QuizImageControllerExceptionHandler {

    @ExceptionHandler(QuizNotFoundException.class)
    public ResponseEntity<Failure> handleQuizNotFound(QuizNotFoundException ex) {
        return Failure.of(HttpStatus.NOT_FOUND,
                ApiError.of("QUIZ_NOT_FOUND", Map.of("id", ex.getQuizId()))).toResponseEntity();
    }

    @ExceptionHandler(QuestionNotFoundException.class)
    public ResponseEntity<Failure> handleQuestionNotFound(QuestionNotFoundException ex) {
        return Failure.of(HttpStatus.NOT_FOUND,
                ApiError.of("QUESTION_NOT_FOUND", Map.of("id", ex.getQuestionId()))).toResponseEntity();
    }

    @ExceptionHandler(OptionNotFoundException.class)
    public ResponseEntity<Failure> handleOptionNotFound(OptionNotFoundException ex) {
        return Failure.of(HttpStatus.NOT_FOUND,
                ApiError.of("OPTION_NOT_FOUND", Map.of("id", ex.getOptionId()))).toResponseEntity();
    }

    @ExceptionHandler(NoAccessToQuizException.class)
    public ResponseEntity<Failure> handleNoAccess(NoAccessToQuizException ex) {
        return Failure.of(HttpStatus.FORBIDDEN,
                ApiError.of("NO_ACCESS_TO_QUIZ", Map.of("id", ex.getQuizId()))).toResponseEntity();
    }

    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<Failure> handleInvalidImage(InvalidImageException ex) {
        return Failure.of(HttpStatus.BAD_REQUEST,
                ApiError.of("INVALID_IMAGE", Map.of("message", ex.getMessage()))).toResponseEntity();
    }

    @ExceptionHandler(AvatarTooLargeException.class)
    public ResponseEntity<Failure> handleTooLarge(AvatarTooLargeException ex) {
        // Reuse AVATAR_TOO_LARGE — the FE already has a resolver that
        // interpolates maxBytes, and the user-facing meaning is identical.
        return Failure.of(HttpStatus.PAYLOAD_TOO_LARGE,
                ApiError.of("AVATAR_TOO_LARGE", Map.of("maxBytes", ex.getMaxBytes()))).toResponseEntity();
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Failure> handleSpringMaxUpload(MaxUploadSizeExceededException ex) {
        return Failure.of(HttpStatus.PAYLOAD_TOO_LARGE,
                ApiError.of("AVATAR_TOO_LARGE", Map.of("maxBytes", ex.getMaxUploadSize()))).toResponseEntity();
    }
}
