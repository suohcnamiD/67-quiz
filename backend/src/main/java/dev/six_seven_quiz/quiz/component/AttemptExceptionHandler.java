package dev.six_seven_quiz.quiz.component;

import dev.six_seven_quiz.quiz.controller.AttemptController;
import dev.six_seven_quiz.quiz.exception.*;
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

@RestControllerAdvice(assignableTypes = AttemptController.class)
@Order(1)
public class AttemptExceptionHandler {

    @ExceptionHandler(QuizNotFoundException.class)
    public ResponseEntity<Failure> handleQuizNotFound(QuizNotFoundException exception) {
        return Failure.of(HttpStatus.NOT_FOUND, ApiError.of("QUIZ_NOT_FOUND", Map.of("id", exception.getQuizId()))).toResponseEntity();
    }

    @ExceptionHandler(AttemptNotFoundException.class)
    public ResponseEntity<Failure> handleAttemptNotFound(AttemptNotFoundException exception) {
        return Failure.of(HttpStatus.NOT_FOUND, ApiError.of("ATTEMPT_NOT_FOUND", Map.of("id", exception.getAttemptId()))).toResponseEntity();
    }

    @ExceptionHandler(NoAccessToAttemptException.class)
    public ResponseEntity<Failure> handleNoAccessToAttempt(NoAccessToAttemptException exception) {
        return Failure.of(HttpStatus.FORBIDDEN, ApiError.of("NO_ACCESS_TO_ATTEMPT", Map.of("id", exception.getAttemptId()))).toResponseEntity();
    }

    @ExceptionHandler(QuestionNotFoundException.class)
    public ResponseEntity<Failure> handleQuestionNotFound(QuestionNotFoundException exception) {
        return Failure.of(HttpStatus.NOT_FOUND, ApiError.of("QUESTION_NOT_FOUND", Map.of("id", exception.getQuestionId()))).toResponseEntity();
    }

    @ExceptionHandler(OptionNotFoundException.class)
    public ResponseEntity<Failure> handleOptionNotFound(OptionNotFoundException exception) {
        return Failure.of(HttpStatus.NOT_FOUND, ApiError.of("OPTION_NOT_FOUND", Map.of("id", exception.getOptionId()))).toResponseEntity();
    }

    @ExceptionHandler(AttemptFinishedException.class)
    public ResponseEntity<Failure> handleAttemptFinished(AttemptFinishedException exception) {
        return Failure.of(HttpStatus.BAD_REQUEST, ApiError.of("ATTEMPT_ALREADY_FINISHED")).toResponseEntity();
    }
}
