package in.co.bel.acempp.dto.ndt;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Data
@Builder
public class NdtDetailsResponse {
    private String formId;
    private String batchId;
    private String batchType;
    private Status formStatus;
    private PersonInfo submittedBy;
    private LocalDateTime submittedAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private PersonInfo updatedBy;
    private LocalDateTime updatedAt;
    private List<Map<String, Object>> motors;
    private Boolean allMotorsApproved;
    private int pendingMotorCount;
    private int approvedMotorCount;
    private int rejectedMotorCount;
    private int inProgressMotorCount;
    private int totalMotorCount;
    private List<MotorStatusResponse> motorStatuses;
}
