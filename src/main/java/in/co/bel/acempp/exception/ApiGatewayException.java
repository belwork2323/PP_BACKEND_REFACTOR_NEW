package in.co.bel.acempp.exception;

public class ApiGatewayException extends RuntimeException {
    public ApiGatewayException(String message) {
        super(message);
    }

    public ApiGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
