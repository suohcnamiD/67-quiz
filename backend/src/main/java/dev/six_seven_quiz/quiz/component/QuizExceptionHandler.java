package dev.six_seven_quiz.quiz.component;

import dev.six_seven_quiz.quiz.controller.QuizController;
import dev.six_seven_quiz.quiz.exception.*;
import dev.six_seven_quiz.shared.dto.Failure;
import dev.six_seven_quiz.shared.dto.error.ApiError;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Order(1)
public class QuizExceptionHandler {

    @ExceptionHandler(QuizNotFoundException.class)
    public ResponseEntity<Failure> handleQuizNotFound(QuizNotFoundException exception) {
        return Failure.of(HttpStatus.NOT_FOUND, ApiError.of("QUIZ_NOT_FOUND", Map.of("id", exception.getQuizId()))).toResponseEntity();
    }

    @ExceptionHandler(NoAccessToAttemptException.class)
    public ResponseEntity<Failure> handleNoAccessToAttempt(NoAccessToAttemptException exception) {
        return Failure.of(HttpStatus.FORBIDDEN, ApiError.of("NO_ACCESS_TO_ATTEMPT", Map.of("id", exception.getAttemptId()))).toResponseEntity();
    }

    @ExceptionHandler(AttemptNotFoundException.class)
    public ResponseEntity<Failure> handleAttemptNotFound(AttemptNotFoundException exception) {
        return Failure.of(HttpStatus.NOT_FOUND, ApiError.of("ATTEMPT_NOT_FOUND", Map.of("id", exception.getAttemptId()))).toResponseEntity();
    }

    @ExceptionHandler(AttemptFinishedException.class)
    public ResponseEntity<Failure> handleAttemptFinished(AttemptFinishedException exception) {
        return Failure.of(HttpStatus.BAD_REQUEST, ApiError.of("ATTEMPT_ALREADY_FINISHED")).toResponseEntity();
    }

    @ExceptionHandler(QuestionNotFoundException.class)
    public ResponseEntity<Failure> handleQuestionNotFound(QuestionNotFoundException exception) {
        return Failure.of(HttpStatus.NOT_FOUND, ApiError.of("QUESTION_NOT_FOUND", Map.of("id", exception.getQuestionId()))).toResponseEntity();
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
