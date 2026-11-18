package in.co.bel.acempp.dto.casepreparation;

import in.co.bel.acempp.dto.batch.BatchRequest;
import in.co.bel.acempp.dto.common.BatchFormRequest;
import in.co.bel.acempp.enums.Status;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MotorApprovalResponse extends BatchFormRequest {

    private String motorId;
    private String division;
    private String subType;
    private Status actionType;
    private Status status;
    private Status motorSubmissionStatus;
    private Status batchStatus;
    private Status divisionStatus;

    private boolean allDivisionsApproved;
    private String unlockedDivision;
    private boolean allMotorsApproved;

    private int pendingMotorCount;
    private int approvedMotorCount;
    private int rejectedMotorCount;
    private int totalMotorCount;

    private String remarks;
    private String rejectionReason;
    private String actionBy;
    private LocalDateTime actionAt;
}
