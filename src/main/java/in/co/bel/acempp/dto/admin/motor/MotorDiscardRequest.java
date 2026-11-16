package in.co.bel.acempp.dto.admin.motor;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MotorDiscardRequest {
    @NotBlank
    private String motorId;
    @NotBlank
    private String reason;
}
