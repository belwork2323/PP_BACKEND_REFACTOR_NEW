package in.co.bel.acempp.dto.casepreparation;

import in.co.bel.acempp.dto.batch.BatchRequest;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.enums.Status;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;


@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CasePreparationResponse extends CreateCasePreparationRequest {

    private String batchType;
    private Status status;
    private Boolean allMotorsApproved;

    // Audit info (using PersonInfo for richer detail, or String if preferred)
    private PersonInfo submittedBy;
    private LocalDateTime submittedAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    // Motor Summary Counts
    private int pendingMotorCount;
    private int approvedMotorCount;
    private int rejectedMotorCount;
    private int inProgressMotorCount;
    private int totalMotorCount;

    // Lists
    private List<MotorStatusResponse> motorStatuses;
    private List<String> discardedMotorIds;

    private CasePreparationDetailsRequest casePreparationDetails;

  }