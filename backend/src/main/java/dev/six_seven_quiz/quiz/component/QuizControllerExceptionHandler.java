package dev.six_seven_quiz.quiz.component;

import dev.six_seven_quiz.quiz.controller.QuizController;
import dev.six_seven_quiz.quiz.exception.NoAccessToQuizException;
import dev.six_seven_quiz.quiz.exception.QuizNotFoundException;
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

@RestControllerAdvice(assignableTypes = QuizController.class)
@Order(1)
public class QuizControllerExceptionHandler {

    @ExceptionHandler(QuizNotFoundException.class)
    @ApiResponse(responseCode = "404", description = "Resource not found — see errors[].code",
            content = @Content(schema = @Schema(implementation = Failure.class)))
    public ResponseEntity<Failure> handleQuizNotFound(QuizNotFoundException exception) {
        return Failure.of(HttpStatus.NOT_FOUND, ApiError.of("QUIZ_NOT_FOUND", Map.of("id", exception.getQuizId()))).toResponseEntity();
    }

    @ExceptionHandler(NoAccessToQuizException.class)
    @ApiResponse(responseCode = "403", description = "Access denied — see errors[].code",
            content = @Content(schema = @Schema(implementation = Failure.class)))
    public ResponseEntity<Failure> handleNoAccess(NoAccessToQuizException exception) {
        return Failure.of(HttpStatus.FORBIDDEN, ApiError.of("NO_ACCESS_TO_QUIZ", Map.of("id", exception.getQuizId()))).toResponseEntity();
    }
}
