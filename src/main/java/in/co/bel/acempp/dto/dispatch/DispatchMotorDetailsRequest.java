package in.co.bel.acempp.dto.dispatch;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchMotorDetailsRequest {

    @NotBlank(message = "batchId is required")
    private String batchId;

    /** Optional — when set, only that motor is returned. */
    private String motorId;
}
