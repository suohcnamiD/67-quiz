package dev.six_seven_quiz.quiz.component;

import dev.six_seven_quiz.quiz.exception.BlankIndexedOptionException;
import dev.six_seven_quiz.quiz.exception.NoAccessToQuizException;
import dev.six_seven_quiz.quiz.exception.QuizNotFoundException;
import dev.six_seven_quiz.shared.dto.Failure;
import dev.six_seven_quiz.shared.dto.error.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class QuizExceptionHandler {

    @ExceptionHandler(QuizNotFoundException.class)
    public ResponseEntity<Failure> handleQuizNotFound(QuizNotFoundException exception) {
        return Failure.of(HttpStatus.NOT_FOUND, ApiError.of("QUIZ_NOT_FOUND", Map.of("id", exception.getQuizId()))).toResponseEntity();
    }

    @ExceptionHandler(NoAccessToQuizException.class)
    public ResponseEntity<Failure> handleNoAccess(NoAccessToQuizException exception) {
        return Failure.of(HttpStatus.FORBIDDEN, ApiError.of("NO_ACCESS_TO_QUIZ", Map.of("id", exception.getQuizId()))).toResponseEntity();
    }

    @ExceptionHandler(BlankIndexedOptionException.class)
    public ResponseEntity<Failure> handleBlankIndexedOption(BlankIndexedOptionException exception) {
        return Failure.of(HttpStatus.BAD_REQUEST, ApiError.of("BLANK_OPTION_TEXT", Map.of("index", exception.getOptionIndex()))).toResponseEntity();
    }
}
