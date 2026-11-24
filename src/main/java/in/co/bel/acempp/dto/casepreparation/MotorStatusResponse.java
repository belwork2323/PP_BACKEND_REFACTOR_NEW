package in.co.bel.acempp.dto.casepreparation;

import in.co.bel.acempp.enums.Status;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotorStatusResponse {

    private String motorId;

    private String division;

    private String subType;

    private Status motorSubmissionType;

    private Status motorSubmissionStatus;
    private Boolean locked;
    private String rejectionReason;
}
