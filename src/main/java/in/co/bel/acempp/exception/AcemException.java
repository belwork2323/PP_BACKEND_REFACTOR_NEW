package in.co.bel.acempp.exception;

import in.co.bel.acempp.enums.ErrorCode;
import lombok.Getter;

@Getter
public class AcemException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Object details;

    public AcemException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public AcemException(ErrorCode errorCode, String message, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

}
