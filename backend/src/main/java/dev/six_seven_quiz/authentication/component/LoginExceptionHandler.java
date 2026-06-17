package dev.six_seven_quiz.authentication.component;

import dev.six_seven_quiz.authentication.AuthenticationController;
import dev.six_seven_quiz.authentication.AuthenticationStateController;
import dev.six_seven_quiz.authentication.exception.UserNotAuthenticatedException;
import dev.six_seven_quiz.shared.dto.Failure;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {AuthenticationController.class, AuthenticationStateController.class})
@Order(1)
public class LoginExceptionHandler {

    @ExceptionHandler(UserNotAuthenticatedException.class)
    public ResponseEntity<Failure> handleUserNotAuthenticatedException(UserNotAuthenticatedException exception) {
        return Failure.status(HttpStatus.UNAUTHORIZED).toResponseEntity();
    }
}
