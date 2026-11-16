package in.co.bel.acempp.dto.castingcuring;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CastingCuringFormResponse {
    private String formId;
    private String batchId;
    private Long subDepartmentId;
    private Status formSubmissionType;
    private Status status;

    // Unique from Form/List responses
    private Integer motorCount;

    // Detailed view fields (will be null/omitted in simple summary responses)
    private ProjectInfo project;
    private List<CastingCuringMotor> motors;
    private Boolean allMotorsApproved;

    // Audit & User Info (using PersonInfo or fallback to String if needed)
    private PersonInfo submittedBy; // Can hold person details, or you can map a string if needed
    private LocalDateTime submittedAt;

    private String createdBy;
    private LocalDateTime createdAt;

    private String lastUpdatedBy;
    private LocalDateTime lastUpdatedAt;

    // Counter statistics
    private int pendingMotorCount;
    private int approvedMotorCount;
    private int rejectedMotorCount;
    private int inProgressMotorCount;
    private int totalMotorCount;

    private List<MotorStatusResponse> motorStatuses;

    /** Motor IDs discarded on this batch (admin); UI must show DISCARDED over APPROVED. */
    private List<String> discardedMotorIds;
}
