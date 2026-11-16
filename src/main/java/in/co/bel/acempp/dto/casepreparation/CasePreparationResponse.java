package in.co.bel.acempp.dto.casepreparation;

import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CasePreparationResponse {

    private String formId;
    private String batchId;
    private String batchType;
    private Long subDepartmentId;

    private Status formSubmissionType;
    private Status status;

    private String submittedBy;
    private LocalDateTime submittedAt;


    private String updatedBy;
    private LocalDateTime updatedAt;

    // Motor Summary
    private int pendingMotorCount;
    private int approvedMotorCount;
    private int rejectedMotorCount;
    private int inProgressMotorCount;
    private int totalMotorCount;

    // Individual Motor Statuses
    private List<MotorStatusResponse> motorStatuses;

    private CasePreparationDetails casePreparationDetails;
}