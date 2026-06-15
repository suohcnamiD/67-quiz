package dev.six_seven_quiz.shared.component;

import dev.six_seven_quiz.shared.dto.Failure;
import dev.six_seven_quiz.shared.dto.error.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Failure> handleMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException exception) {
        return Failure.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).toResponseEntity();
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Failure> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException exception) {
        return Failure.status(METHOD_NOT_ALLOWED).toResponseEntity();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Failure> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        //logger.warn("Malformed request received", exception);

        Throwable cause = exception.getCause();
        if (cause instanceof InvalidFormatException invalidFormatException) {
            String fieldName = invalidFormatException.getPath().stream()
                    .map(JacksonException.Reference::getPropertyName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("."));

            return Failure.invalidFormat(fieldName).toResponseEntity();
        }

        return Failure.status(BAD_REQUEST).toResponseEntity();
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<?> handlerNotFoundException(NoHandlerFoundException exception) {
        return Failure.status(NOT_FOUND).toResponseEntity();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Failure> handleValidationExceptions(MethodArgumentNotValidException exception) {
        return Failure.of(
                HttpStatus.BAD_REQUEST,
                exception.getFieldErrors().stream().map(fieldError -> {
                    Map<String, Object> details = new HashMap<>();
                    details.put("field", fieldError.getField());
                    if (fieldError.getRejectedValue() != null) details.put("rejectedValue", fieldError.getRejectedValue());
                    if (fieldError.getDefaultMessage() != null) details.put("message", fieldError.getDefaultMessage());
                    return ApiError.of("VALIDATION_ERROR", details);
                }).toList()
        ).toResponseEntity();
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Failure> handleHandlerMethodValidationException(HandlerMethodValidationException exception) {
        return Failure.of(
                HttpStatus.BAD_REQUEST,
                exception.getValueResults().stream().map(violation -> {
                    Map<String, Object> details = new HashMap<>();
                    details.put("field", violation.getMethodParameter().getParameterName());
                    if (violation.getArgument() != null) details.put("rejectedValue", violation.getArgument());
                    return ApiError.of("VALIDATION_ERROR", details);
                }).toList()
        ).toResponseEntity();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Failure> handleAllExceptions(Exception exception) {
        logger.error("Unhandled exception occurred", exception);
        return Failure.status(INTERNAL_SERVER_ERROR).toResponseEntity();
    }
}
