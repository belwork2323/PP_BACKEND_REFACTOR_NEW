package in.co.bel.acempp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AcemResponse<T> {
    // Common
    private boolean success;
    private int code;
    private String message;

    // Success only
    private T data;

    // Error only
    private String errorCode;
    private Object errorDetails;
    private String requestId;
    private String timestamp;
  //  private String supportUrl;

}