package in.co.bel.acempp.dto.rawmaterialpreparation;

import in.co.bel.acempp.enums.StageType;
import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremixApprovalResponse {
    private String formId;

    private String batchId;

    private Integer premixNo;

    private String division;

    private String subType;

    private Status actionType;

    private Status status;

    private Status premixSubmissionStatus;

    private Status batchStatus;

    private Status divisionStatus;

    private boolean allDivisionsApproved;

    private String unlockedDivision;

    private boolean allPremixesApproved;

    private int pendingPremixCount;

    private int approvedPremixCount;

    private int rejectedPremixCount;

    private int totalPremixCount;

    private StageType stageType;

    private int inProgressPremixCount;

    private int pendingFinalMixCount;
    private int approvedFinalMixCount;
    private int rejectedFinalMixCount;
    private int inProgressFinalMixCount;
    private int totalFinalMixCount;

    private String remarks;

    private String rejectionReason;

    private String actionBy;

    private LocalDateTime actionAt;
}
