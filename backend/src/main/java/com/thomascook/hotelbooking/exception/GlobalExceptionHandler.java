package com.thomascook.hotelbooking.exception;

import com.thomascook.hotelbooking.dto.response.ApiError;
import com.thomascook.hotelbooking.dto.response.FieldErrorDetail;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Translates every exception into the same {@link ApiError} JSON:
 * {timestamp, status, error, message, fieldErrors}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Business errors: 404 not found, 409 no availability / invalid state. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex) {
        return build(ex.getStatus(), ex.getMessage(), List.of());
    }

    /** @Valid failures on request bodies (MethodArgumentNotValidException is a BindException) and query-param records. */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiError> handleBindException(BindException ex) {
        List<FieldErrorDetail> details = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> details.add(new FieldErrorDetail(fe.getField(), messageOf(fe))));
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> details.add(new FieldErrorDetail(ge.getObjectName(), ge.getDefaultMessage())));
        return validationFailed(details);
    }

    /** Violations on @Validated parameters (path variables, simple request params). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldErrorDetail> details = ex.getConstraintViolations().stream()
                .map(v -> {
                    String path = v.getPropertyPath().toString();
                    return new FieldErrorDetail(path.substring(path.lastIndexOf('.') + 1), v.getMessage());
                })
                .collect(Collectors.toList());
        return validationFailed(details);
    }

    /** Broken JSON, or a value of the wrong type/format (e.g. checkInDate: "31-12-2026"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof MismatchedInputException mie && !mie.getPath().isEmpty()) {
            String field = mie.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining("."));
            return validationFailed(List.of(new FieldErrorDetail(field, invalidValueMessage(field, null))));
        }
        return build(HttpStatus.BAD_REQUEST, "Malformed request body. Please check the JSON syntax.", List.of());
    }

    /** e.g. /api/hotels/abc or ?checkInDate=not-a-date. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = invalidValueMessage(ex.getName(), ex.getValue());
        return validationFailed(List.of(new FieldErrorDetail(ex.getName(), message)));
    }

    /** Lock wait timed out: the hotel is being booked heavily right now. Safe for the client to retry. */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleLockFailure(PessimisticLockingFailureException ex) {
        log.warn("Could not acquire booking lock: {}", ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE,
                "Many guests are booking this hotel right now. Please try again in a moment.", List.of());
    }

    /** Last line of defence for DB constraint violations (e.g. a duplicate booking reference). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return build(HttpStatus.CONFLICT, "The request conflicts with existing data. Please try again.", List.of());
    }

    /**
     * Everything else. Spring's own web exceptions (405, 415, 404 for unknown URLs, missing params...)
     * implement {@link ErrorResponse} and already know their status; anything unknown is a 500 with a generic message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAny(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            String detail = errorResponse.getBody().getDetail();
            return build(errorResponse.getStatusCode(), detail != null ? detail : "Request failed", List.of());
        }
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.", List.of());
    }

    // ---- helpers ----

    private ResponseEntity<ApiError> validationFailed(List<FieldErrorDetail> details) {
        List<FieldErrorDetail> sorted = details.stream()
                .sorted(Comparator.comparing(FieldErrorDetail::field).thenComparing(FieldErrorDetail::message))
                .toList();
        String message = sorted.size() == 1 ? sorted.get(0).message() : "Validation failed. Please correct the highlighted fields.";
        return build(HttpStatus.BAD_REQUEST, message, sorted);
    }

    private ResponseEntity<ApiError> build(HttpStatusCode status, String message, List<FieldErrorDetail> fieldErrors) {
        String reason = status instanceof HttpStatus httpStatus ? httpStatus.getReasonPhrase() : "Error";
        ApiError body = new ApiError(Instant.now(), status.value(), reason, message, fieldErrors);
        return ResponseEntity.status(status).body(body);
    }

    private String messageOf(FieldError fieldError) {
        if (fieldError.isBindingFailure()) {
            return invalidValueMessage(fieldError.getField(), fieldError.getRejectedValue());
        }
        return fieldError.getDefaultMessage();
    }

    private String invalidValueMessage(String field, Object rejectedValue) {
        String value = rejectedValue == null ? "" : " '" + rejectedValue + "'";
        String hint = field.toLowerCase().endsWith("date") ? " Dates must use the format yyyy-MM-dd." : "";
        return "Invalid value" + value + " for " + field + "." + hint;
    }
}
