package in.co.bel.acempp.dto.rawmaterialpreparation;

import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PreparationResponse {
    private String batchId;
    private String formId;
    private Status formSubmissionType;
    private int premixCount;
    private int totalSolidMaterials;
    private int totalLiquidMaterials;
    private boolean weightmentSheetIncluded;
    private Status status;

    private int pendingPremixCount;

    private int approvedPremixCount;

    private int rejectedPremixCount;

    private int inProgressPremixCount;

    private int totalPremixCount;

    private List<PremixStatusResponse> premixStatuses;


    private String submittedBy;
    private LocalDateTime submittedAt;

    private String lastUpdatedBy;
    private LocalDateTime lastUpdatedAt;
}