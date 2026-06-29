package dev.six_seven_quiz.quiz.component;

import dev.six_seven_quiz.quiz.controller.QuizRatingController;
import dev.six_seven_quiz.quiz.exception.InvalidRatingException;
import dev.six_seven_quiz.quiz.exception.QuizNotFoundException;
import dev.six_seven_quiz.quiz.exception.RatingNotEligibleException;
import dev.six_seven_quiz.shared.dto.Failure;
import dev.six_seven_quiz.shared.dto.error.ApiError;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = QuizRatingController.class)
@Order(1)
public class QuizRatingControllerExceptionHandler {

    @ExceptionHandler(QuizNotFoundException.class)
    public ResponseEntity<Failure> handleQuizNotFound(QuizNotFoundException exception) {
        return Failure.of(HttpStatus.NOT_FOUND, ApiError.of("QUIZ_NOT_FOUND", Map.of("id", exception.getQuizId()))).toResponseEntity();
    }

    @ExceptionHandler(RatingNotEligibleException.class)
    public ResponseEntity<Failure> handleNotEligible(RatingNotEligibleException exception) {
        return Failure.of(HttpStatus.FORBIDDEN, ApiError.of("RATING_NOT_ELIGIBLE", Map.of("quizId", exception.getQuizId()))).toResponseEntity();
    }

    @ExceptionHandler(InvalidRatingException.class)
    public ResponseEntity<Failure> handleInvalidRating(InvalidRatingException exception) {
        return Failure.of(HttpStatus.BAD_REQUEST, ApiError.of("INVALID_RATING", Map.of(
                "minimum", exception.getMinimum(),
                "maximum", exception.getMaximum()
        ))).toResponseEntity();
    }
}
