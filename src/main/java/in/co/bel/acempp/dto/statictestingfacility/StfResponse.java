package in.co.bel.acempp.dto.statictestingfacility;

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
public class StfResponse {
    private String formId;
    private String batchId;
    private Long subDepartmentId;
    private Status formSubmissionType;
    private Status formStatus;

    // Audit & submission metadata (covers both *At and *On naming from existing DTOs)
    private PersonInfo createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime createdOn;

    private PersonInfo submittedBy; // supports String or PersonInfo depending on endpoint
    private LocalDateTime submittedAt;
    private LocalDateTime submittedOn;

    private PersonInfo updatedBy; // supports String or PersonInfo depending on endpoint
    private LocalDateTime updatedAt;
    private LocalDateTime updatedOn;

    // Motors & counts
    private int motorCount;
    private List<StaticMotorData> motors; // supports List<MotorIdDto> or List<StaticMotorData>
    private boolean allMotorsApproved;
    private int pendingMotorCount;
    private int approvedMotorCount;
    private int rejectedMotorCount;
    private int inProgressMotorCount;
    private int totalMotorCount;
    private List<MotorStatusResponse> motorStatuses;
}
