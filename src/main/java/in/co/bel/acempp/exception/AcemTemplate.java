package in.co.bel.acempp.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import in.co.bel.acempp.dto.AcemResponse;

public class AcemTemplate {

    // 200 OK
    public static <T> ResponseEntity<AcemResponse<T>> success(T data, String message) {
        return buildResponse(HttpStatus.OK, data, message);
    }

    // 201 CREATED
    public static <T> ResponseEntity<AcemResponse<T>> created(T data, String message) {
        return buildResponse(HttpStatus.CREATED, data, message);
    }

    public static <T> ResponseEntity<AcemResponse<T>> validationError(
            Object errorDetails, String message, String errorCode) {

        AcemResponse<T> response = AcemResponse.<T>builder()
                .success(false)
                .code(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .errorCode(errorCode)
                .errorDetails(errorDetails)
                .timestamp(LocalDateTime.now().toString())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 500 INTERNAL SERVER ERROR
    public static <T> ResponseEntity<AcemResponse<T>> error(String message, String errorCode) {
        AcemResponse<T> response = AcemResponse.<T>builder()
                .success(false)
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now().toString())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // shared builder
    private static <T> ResponseEntity<AcemResponse<T>> buildResponse(
            HttpStatus status, T data, String message) {

        AcemResponse<T> response = AcemResponse.<T>builder()
                .success(true)
                .code(status.value())
                .message(message)
                .timestamp(LocalDateTime.now().toString())
                .errorCode(null)
                .data(data)
                .build();

        return ResponseEntity.status(status).body(response);
    }
}
