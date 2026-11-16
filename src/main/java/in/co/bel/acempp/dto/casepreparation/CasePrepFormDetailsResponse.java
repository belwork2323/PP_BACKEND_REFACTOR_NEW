package in.co.bel.acempp.dto.casepreparation;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CasePrepFormDetailsResponse {
    private String formId;
    private String batchId;
    private String batchType;
    private Long subDepartmentId;
    private Status formSubmissionType;
    private Status status;

    private Boolean allMotorsApproved;

    private int pendingMotorCount;
    private int approvedMotorCount;
    private int rejectedMotorCount;
    private int inProgressMotorCount;
    private int totalMotorCount;

    private List<MotorStatusResponse> motorStatuses;

    /** Motor IDs discarded on this batch (admin); UI must show DISCARDED over APPROVED. */
    private List<String> discardedMotorIds;

    private String createdBy;
    private LocalDateTime createdAt;
    private PersonInfo submittedBy;
    private LocalDateTime submittedAt;


    private CasePreparationDetails casePreparationDetails;
}