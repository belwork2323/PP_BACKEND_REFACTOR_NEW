package in.co.bel.acempp.dto.statictestingfacility;

import in.co.bel.acempp.dto.statictestingfacility.StfPayloads.StaticTestingDetails;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.enums.StfMotorSubType;
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
public class StfMotorRequest {
    @NotBlank
    private String motorId;
    @NotNull
    private StfMotorSubType subType;
    @NotBlank
    private String stfTestNo;
    @NotNull
    private Status motorSubmissionType;
    @Valid
    private StaticTestingDetails staticTestingDetails;
}
