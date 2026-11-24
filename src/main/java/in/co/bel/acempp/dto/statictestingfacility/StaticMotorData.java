package in.co.bel.acempp.dto.statictestingfacility;

import in.co.bel.acempp.dto.statictestingfacility.StfPayloads.StaticTestingDetails;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.enums.StfMotorSubType;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StaticMotorData extends StfMotorRequest {
    private Status motorSubmissionStatus;       // IN_PROGRESS / WAITING_FOR_APPROVAL / APPROVED / REJECTED
    private String rejectionReason;
    private String stfTestNo;
    private String bemNo;
}
