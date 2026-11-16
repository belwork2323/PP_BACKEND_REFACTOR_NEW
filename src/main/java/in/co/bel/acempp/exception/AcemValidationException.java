package in.co.bel.acempp.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class AcemValidationException extends RuntimeException {

    private final Map<String, Object> errors;

    public AcemValidationException(String message, Map<String, Object> errors) {
        super(message);
        this.errors = errors;
    }
}
