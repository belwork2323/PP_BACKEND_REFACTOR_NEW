package in.co.bel.acempp.dto.dispatch;

import in.co.bel.acempp.dto.dispatch.DispatchPayloads.DispatchDetails;
import in.co.bel.acempp.enums.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchMotorRequest {
    @NotBlank
    private String motorId;
    @NotNull
    private Status motorSubmissionType;
    @Valid
    private DispatchDetails dispatchDetails;
}
