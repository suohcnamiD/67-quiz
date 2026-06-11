package dev.six_seven_quiz.quiz.component;

import dev.six_seven_quiz.quiz.controller.AttemptController;
import dev.six_seven_quiz.quiz.controller.QuizController;
import dev.six_seven_quiz.quiz.exception.*;
import dev.six_seven_quiz.quiz.model.Option;
import dev.six_seven_quiz.shared.dto.Failure;
import dev.six_seven_quiz.shared.dto.error.ApiError;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = AttemptController.class)
@Order(1)
public class AttemptExceptionHandler {

    @ExceptionHandler(OptionNotFoundException.class)
    public ResponseEntity<Failure> handleOptionNotFound(OptionNotFoundException exception) {
        return Failure.of(HttpStatus.NOT_FOUND, ApiError.of("OPTION_NOT_FOUND", Map.of("id", exception.getOptionId()))).toResponseEntity();
    }
}
