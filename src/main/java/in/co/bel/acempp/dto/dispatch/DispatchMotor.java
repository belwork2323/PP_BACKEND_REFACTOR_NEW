package in.co.bel.acempp.dto.dispatch;

import in.co.bel.acempp.dto.dispatch.DispatchPayloads.DispatchDetails;
import in.co.bel.acempp.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchMotor {
    private String motorId;
    private Status motorSubmissionType;
    private Status motorSubmissionStatus;
    private String rejectionReason;
    private DispatchDetails dispatchDetails;
}
