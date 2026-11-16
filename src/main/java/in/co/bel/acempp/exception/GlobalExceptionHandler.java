package in.co.bel.acempp.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.enums.ErrorCode;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AcemResponse<Object>> handleUnreadableRequest(
            HttpMessageNotReadableException ex) {

        if (hasCause(ex, DateTimeParseException.class)) {
            return invalidDateResponse();
        }

        String errorMessage = "Malformed JSON request or type mismatch";
        Throwable cause = ex.getCause();

        // 🔍 Log the exact underlying exception type and message to your console
        log.error("JSON Parse Exception Type: {}, Cause Message: {}",
                cause != null ? cause.getClass().getName() : "None",
                cause != null ? cause.getMessage() : ex.getMessage(),
                ex);

        if (cause instanceof MismatchedInputException) {
            MismatchedInputException mix = (MismatchedInputException) cause;
            String fieldName = "unknown";

            if (mix.getPath() != null && !mix.getPath().isEmpty()) {
                JsonMappingException.Reference lastRef = mix.getPath().get(mix.getPath().size() - 1);
                if (lastRef.getFieldName() != null) {
                    fieldName = lastRef.getFieldName();
                } else if (lastRef.getIndex() >= 0) {
                    fieldName = "index[" + lastRef.getIndex() + "]";
                }
            }
            errorMessage = String.format("Invalid value or type mismatch for field '%s'.", fieldName);
        }

        AcemResponse<Object> response = AcemResponse.builder()
                .success(false)
                .code(HttpStatus.BAD_REQUEST.value())
                .message(errorMessage)
                .errorCode(ErrorCode.INVALID_REQUEST.name())
                .requestId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now().toString())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<AcemResponse<Object>> handleInvalidDate(
            DateTimeParseException ex) {
        return invalidDateResponse();
    }

    private ResponseEntity<AcemResponse<Object>> invalidDateResponse() {
        AcemResponse<Object> response = AcemResponse.builder()
                .success(false)
                .code(HttpStatus.BAD_REQUEST.value())
                .message("Invalid date or date-time format. Expected dd-MM-yyyy, yyyy-MM-dd, "
                        + "dd-MM-yyyy HH:mm:ss, dd-MM-yyyy HH:mm, or an ISO date-time")
                .errorCode(ErrorCode.INVALID_DATE_FORMAT.name())
                .requestId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now().toString())
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) return true;
            current = current.getCause();
        }
        return false;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AcemResponse<Object>> handleValidationException(
            MethodArgumentNotValidException ex) {

        // Keep full field paths (e.g. materials[0].lots[0].certificates[0].fileId)
        // so clients can map errors onto nested form controls.
        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        LinkedHashMap::new,
                        Collectors.toList()))
                .forEach((field, fieldErrors) -> {

                    Optional<FieldError> notBlankError = fieldErrors.stream()
                            .filter(e -> "NotBlank".equals(e.getCode()) || "NotNull".equals(e.getCode()))
                            .findFirst();

                    if (notBlankError.isPresent()) {
                        errors.put(field, notBlankError.get().getDefaultMessage());
                        return;
                    }

                    Optional<FieldError> sizeError = fieldErrors.stream()
                            .filter(e -> "Size".equals(e.getCode()))
                            .findFirst();

                    if (sizeError.isPresent()) {
                        errors.put(field, sizeError.get().getDefaultMessage());
                        return;
                    }

                    errors.put(field, fieldErrors.get(0).getDefaultMessage());
                });

        AcemResponse<Object> response = AcemResponse.builder()
                .success(false)
                .code(HttpStatus.BAD_REQUEST.value())
                .message("Validation failed")
                .errorCode(ErrorCode.VALIDATION_ERROR.name())
                .requestId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now().toString())
                .data(errors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(AcemException.class)
    public ResponseEntity<AcemResponse<Object>> handleAcemException(AcemException ex) {
        AcemResponse<Object> response = AcemResponse.builder()
                .success(false)
                .code(ex.getErrorCode().getStatus().value())
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode().name())
                .requestId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now().toString())
                .data(ex.getDetails())
                .build();

        return ResponseEntity
                .status(ex.getErrorCode().getStatus())
                .body(response);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<AcemResponse<Object>> handleAuthException(AuthException ex) {
        AcemResponse<Object> response = AcemResponse.builder()
                .success(false)
                .code(ex.getErrorCode().getStatus().value())
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode().name())
                .requestId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now().toString())
                .build();

        return ResponseEntity
                .status(ex.getErrorCode().getStatus())
                .body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<AcemResponse<Object>> handleAccessDenied(
            AccessDeniedException ex) {
        AcemResponse<Object> response = AcemResponse.builder()
                .success(false)
                .code(ErrorCode.INSUFFICIENT_PERMISSIONS.getStatus().value())
                .message("Access Denied")
                .errorCode(ErrorCode.INSUFFICIENT_PERMISSIONS.name())
                .requestId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now().toString())
                .build();

        return ResponseEntity
                .status(ErrorCode.INSUFFICIENT_PERMISSIONS.getStatus())
                .body(response);
    }

    @ExceptionHandler(AcemValidationException.class)
    public ResponseEntity<AcemResponse<Object>> handleValidationException(AcemValidationException ex) {
        return AcemTemplate.validationError(
                ex.getErrors(),
                "Validation failed for the request",
                "INVALID_REQUEST"
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body("File size exceeds maximum permitted upload limit!");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AcemResponse<Object>> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception", ex);

        AcemResponse<Object> response = AcemResponse.builder()
                .success(false)
                .code(ErrorCode.INTERNAL_SERVER_ERROR.getStatus().value())
                .message("Unexpected server error")
                .errorCode(ErrorCode.INTERNAL_SERVER_ERROR.name())
                .requestId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now().toString())
                .build();

        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(response);
    }

    @ExceptionHandler(ApiGatewayException.class)
    public ResponseEntity<AcemResponse<Object>> handleBlockchainServiceException(
            ApiGatewayException ex,
            HttpServletRequest request) {

        log.error("Blockchain service exception: {}", ex.getMessage(), ex);

        AcemResponse<Object> response = AcemResponse.builder()
                .success(false)
                .code(HttpStatus.SERVICE_UNAVAILABLE.value())
                .message(ex.getMessage() != null ? ex.getMessage() : "Blockchain service error occurred")
                .errorCode("BLOCKCHAIN_SERVICE_ERROR")
                .requestId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now().toString())
                .build();

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }

}