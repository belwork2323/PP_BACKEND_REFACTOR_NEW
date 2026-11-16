package in.co.bel.acempp.dto.statictestingfacility;

import in.co.bel.acempp.dto.statictestingfacility.StfPayloads.StaticTestingDetails;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.enums.StfMotorSubType;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaticMotorData {
    private String motorId;                     // "EMP300"
    private StfMotorSubType subType;
    private Status motorSubmissionType;         // DRAFT / SUBMIT
    private Status motorSubmissionStatus;       // IN_PROGRESS / WAITING_FOR_APPROVAL / APPROVED / REJECTED
    private String stfTestNo;
    private String bemNo;
    private StaticTestingDetails staticTestingDetails;
}
