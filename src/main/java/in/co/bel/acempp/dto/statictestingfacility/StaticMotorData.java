package in.co.bel.acempp.dto.statictestingfacility;

import in.co.bel.acempp.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StaticMotorData extends StfMotorRequest {
    private Status motorSubmissionStatus;       // IN_PROGRESS / WAITING_FOR_APPROVAL / APPROVED / REJECTED
    private String rejectionReason;
    // stfTestNo is inherited from StfMotorRequest — do not redeclare (Mongo ambiguous mapping).
    private String bemNo;
}
