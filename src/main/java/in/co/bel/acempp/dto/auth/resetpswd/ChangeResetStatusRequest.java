package in.co.bel.acempp.dto.auth.resetpswd;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangeResetStatusRequest {
    @NotBlank
    private String requestId;
    @NotBlank private String action;   // APPROVE / REJECT
    private String remarks;
}
