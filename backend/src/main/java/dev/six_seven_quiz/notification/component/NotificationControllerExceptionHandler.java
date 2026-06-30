package dev.six_seven_quiz.notification.component;

import dev.six_seven_quiz.notification.controller.NotificationController;
import dev.six_seven_quiz.notification.exception.NotificationNotFoundException;
import dev.six_seven_quiz.shared.dto.Failure;
import dev.six_seven_quiz.shared.dto.error.ApiError;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = NotificationController.class)
@Order(1)
public class NotificationControllerExceptionHandler {

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<Failure> handleNotFound(NotificationNotFoundException ex) {
        return Failure.of(HttpStatus.NOT_FOUND, ApiError.of("NOTIFICATION_NOT_FOUND", Map.of("id", ex.getId()))).toResponseEntity();
    }
}
