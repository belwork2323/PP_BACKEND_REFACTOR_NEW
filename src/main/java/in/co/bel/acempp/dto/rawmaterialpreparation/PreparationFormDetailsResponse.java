package in.co.bel.acempp.dto.rawmaterialpreparation;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PreparationFormDetailsResponse {
    private String formId;
    private String batchId;
    private Long subDepartmentId;
    private Status formSubmissionType;
    private Status status;
    private String createdBy;
    private LocalDateTime createdAt;
    private String lastUpdatedBy;
    private LocalDateTime lastUpdatedAt;
    private PersonInfo submittedBy;
    private LocalDateTime submittedAt;

    // Premix Summary
    private int pendingPremixCount;
    private int approvedPremixCount;
    private int rejectedPremixCount;
    private int inProgressPremixCount;
    private int totalPremixCount;

    // Individual Premix Statuses
    private List<PremixStatusResponse> premixStatuses;
    private PreparationDetailsRequest preparationDetails;
}