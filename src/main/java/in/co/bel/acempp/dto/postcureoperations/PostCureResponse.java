package in.co.bel.acempp.dto.postcureoperations;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.enums.PostCureInhibitorType;
import in.co.bel.acempp.enums.PostCureOperationType;
import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostCureResponse {
    private String formId;
    private String batchId;
    private String batchType;
    private Long subDepartmentId;
    private PostCureOperationType operationType;
    private PostCureInhibitorType inhibitorType;
    private Status status;

    private List<PostCureMotor> motors;
    private Boolean allMotorsApproved;

    // Motor Statistics
    private int pendingMotorCount;
    private int approvedMotorCount;
    private int rejectedMotorCount;
    private int inProgressMotorCount;
    private int totalMotorCount;
    private List<MotorStatusResponse> motorStatuses;

    // Audit & Tracking fields
    private PersonInfo submittedBy;
    private LocalDateTime submittedAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
