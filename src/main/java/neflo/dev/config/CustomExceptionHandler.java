package neflo.dev.config;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import lombok.extern.slf4j.Slf4j;
import neflo.dev.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.security.SignatureException;

@RestControllerAdvice
@Slf4j
public class CustomExceptionHandler {

    @ExceptionHandler(value = {NoEntitiesFoundException.class})
    public ResponseEntity<CustomErrorResponse> handleNoEntitiesFound(NoEntitiesFoundException exception) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        CustomErrorResponse response = new CustomErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getErrorCode(),
                exception.getMessage()
        );
        log.error("DriveMeMaybeAPI.ExceptionHandler >> NO ENTITIES FOUND EXCEPTION {}", exception.getErrorCode(), exception);

        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(value = {ValidationException.class})
    public ResponseEntity<CustomErrorResponse> handleValidationException(ValidationException exception) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        CustomErrorResponse response = new CustomErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getErrorCode(),
                exception.getMessage()
        );
        log.error("DriveMeMaybeAPI.ExceptionHandler >> VALIDATION EXCEPTION {}", exception.getErrorCode(), exception);

        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(value = {DatabaseException.class})
    public ResponseEntity<CustomErrorResponse> handleDatabaseException(DatabaseException exception) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        CustomErrorResponse response = new CustomErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getErrorCode(),
                exception.getMessage()
        );
        log.error("DriveMeMaybeAPI.ExceptionHandler >> DATABASE EXCEPTION {}", exception.getErrorCode(), exception);

        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(value = {UnexpectedException.class})
    public ResponseEntity<CustomErrorResponse> handleUnexpectedException(UnexpectedException exception) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        CustomErrorResponse response = new CustomErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getErrorCode(),
                exception.getMessage()
        );
        log.error("DriveMeMaybeAPI.ExceptionHandler >> UNEXPECTED EXCEPTION {}", exception.getErrorCode(), exception);

        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(value = {AuthenticationException.class})
    public ResponseEntity<CustomErrorResponse> handleAuthenticationException(AuthenticationException exception) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        CustomErrorResponse response = new CustomErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getErrorCode(),
                exception.getMessage()
        );
        log.error("DriveMeMaybeAPI.ExceptionHandler >> AUTHENTICATION EXCEPTION {}", exception.getErrorCode(), exception);

        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(value = {JwtException.class, MalformedJwtException.class, ExpiredJwtException.class, BadCredentialsException.class, SignatureException.class})
    public ResponseEntity<CustomErrorResponse> handleFilterExceptions(Exception exception) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        String errorCode = "authorizationException";
        CustomErrorResponse response = new CustomErrorResponse(
                HttpStatus.NOT_FOUND,
                errorCode,
                exception.getMessage()
        );
        log.error("DriveMeMaybeAPI.ExceptionHandler >> AUTHORIZATION EXCEPTION {}", errorCode, exception);

        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(value = {Exception.class})
    public ResponseEntity<CustomErrorResponse> handleGenericException(Exception exception) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String errorCode = "generic-exception";
        String message = "An unexpected error occurred, we are working on it.";
        CustomErrorResponse response = new CustomErrorResponse(
                HttpStatus.NOT_FOUND,
                errorCode,
                message
        );
        log.error("DriveMeMaybeAPI.ExceptionHandler >> GENERIC EXCEPTION {}", errorCode, exception);

        return new ResponseEntity<>(response, status);
    }

}
