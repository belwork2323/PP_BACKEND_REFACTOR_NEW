package in.co.bel.acempp.dto.dispatch;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DispatchDetailsResponse {
    private String formId;
    private String batchId;
    private String batchType;
    private Long subDepartmentId;
    private Status formSubmissionType;
    private Status formStatus;
    private List<DispatchMotor> motors;
    private String createdBy;
    private LocalDateTime createdAt;
    private PersonInfo submittedBy;
    private LocalDateTime submittedAt;
    private PersonInfo updatedBy;
    private LocalDateTime updatedAt;
    private Boolean allMotorsApproved;
    private int pendingMotorCount;
    private int approvedMotorCount;
    private int rejectedMotorCount;
    private int inProgressMotorCount;
    private int totalMotorCount;
    private List<MotorStatusResponse> motorStatuses;
}
