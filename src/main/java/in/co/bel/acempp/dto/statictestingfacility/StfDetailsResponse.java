package in.co.bel.acempp.dto.statictestingfacility;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Data
@Builder
public class StfDetailsResponse {
    private String formId;
    private String batchId;
    private Long subDepartmentId;
    private Status formSubmissionType;
    private Status formStatus;
    private PersonInfo submittedBy;
    private LocalDateTime submittedAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private PersonInfo updatedBy;
    private LocalDateTime updatedAt;
    private List<StaticMotorData> motors;
    private boolean allMotorsApproved;
    private int pendingMotorCount;
    private int approvedMotorCount;
    private int rejectedMotorCount;
    private int inProgressMotorCount;
    private int totalMotorCount;
    private List<MotorStatusResponse> motorStatuses;
}
