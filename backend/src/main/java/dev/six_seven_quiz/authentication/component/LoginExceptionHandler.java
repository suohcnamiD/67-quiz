package dev.six_seven_quiz.authentication.component;

import dev.six_seven_quiz.authentication.AuthenticationController;
import dev.six_seven_quiz.authentication.exception.UserNotAuthenticatedException;
import dev.six_seven_quiz.shared.dto.Failure;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthenticationController.class)
@Order(1)
public class LoginExceptionHandler {

    @ExceptionHandler(UserNotAuthenticatedException.class)
    @ApiResponse(responseCode = "401", description = "Authentication required — see errors[].code",
            content = @Content(schema = @Schema(implementation = Failure.class)))
    public ResponseEntity<Failure> handleUserNotAuthenticatedException(UserNotAuthenticatedException exception) {
        return Failure.status(HttpStatus.UNAUTHORIZED).toResponseEntity();
    }
}
