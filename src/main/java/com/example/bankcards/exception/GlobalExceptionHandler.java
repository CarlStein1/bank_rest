package com.example.bankcards.exception;

import com.example.bankcards.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class
            );

    @ExceptionHandler({
            UserNotFoundException.class,
            CardNotFoundException.class,
            NoSuchElementException.class
    })
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Неверный логин или пароль",
                request
        );
    }

    @ExceptionHandler(CardAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            CardAccessDeniedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler({
            InsufficientFundsException.class,
            CardBlockedException.class,
            DuplicateBlockRequestException.class,
            LoginAlreadyExistsException.class,
            IllegalStateException.class
    })
    public ResponseEntity<ApiErrorResponse> handleConflict(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Переданы некорректные данные";
        }

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        String message = exception
                .getConstraintViolations()
                .stream()
                .map(this::formatConstraintViolation)
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Переданы некорректные данные";
        }

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String message =
                "Параметр '"
                        + exception.getName()
                        + "' имеет некорректное значение";

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );
    }

    @ExceptionHandler(
            MissingServletRequestParameterException.class
    )
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        String message =
                "Отсутствует обязательный параметр: "
                        + exception.getParameterName();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Тело запроса отсутствует или содержит некорректный JSON",
                request
        );
    }

    @ExceptionHandler(
            HttpRequestMethodNotSupportedException.class
    )
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        String message =
                "HTTP-метод "
                        + exception.getMethod()
                        + " не поддерживается для этого ресурса";

        return buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                message,
                request
        );
    }

    @ExceptionHandler(
            HttpMediaTypeNotSupportedException.class
    )
    public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Неподдерживаемый тип содержимого запроса",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Непредвиденная ошибка при обработке запроса {}",
                request.getRequestURI(),
                exception
        );

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Произошла внутренняя ошибка сервера",
                request
        );
    }

    private String formatFieldError(
            FieldError fieldError
    ) {
        String errorMessage =
                Objects.requireNonNullElse(
                        fieldError.getDefaultMessage(),
                        "Некорректное значение"
                );

        return fieldError.getField()
                + ": "
                + errorMessage;
    }

    private String formatConstraintViolation(
            ConstraintViolation<?> violation
    ) {
        return violation.getPropertyPath()
                + ": "
                + violation.getMessage();
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        ApiErrorResponse response =
                new ApiErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}