package in.co.bel.acempp.dto.statictestingfacility;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StfCreateResponse {
    private String formId;
    private String batchId;
    private Status formStatus;
    private int motorCount;
    private List<MotorIdDto> motors;           // motorId list
    private String createdBy;
    private LocalDateTime createdOn;
    private String submittedBy;
    private LocalDateTime submittedOn;
    private boolean allMotorsApproved;
    private int pendingMotorCount;
    private int approvedMotorCount;
    private int rejectedMotorCount;
    private int inProgressMotorCount;
    private int totalMotorCount;
    private List<MotorStatusResponse> motorStatuses;
}
