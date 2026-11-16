package in.co.bel.acempp.dto.casepreparation;

import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MotorApprovalResponse {

    private String formId;

    private String batchId;

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
