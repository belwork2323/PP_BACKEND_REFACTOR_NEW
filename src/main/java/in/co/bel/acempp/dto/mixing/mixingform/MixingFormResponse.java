package in.co.bel.acempp.dto.mixing.mixingform;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.mixing.mixingcycle.MixingDetailsDto;
import in.co.bel.acempp.dto.rawmaterialpreparation.PremixStatusResponse;
import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MixingFormResponse {
    private String formId;
    private String batchId;
    private String batchType;
    private Status formSubmissionType;
    private Status status;

    // Unique to Details (will be null in Form response if not needed)
    private Long subDepartmentId;
    private MixingDetailsDto mixingDetails;

    // Counts
    private long premixCount;
    private long pendingPremixCount;
    private long approvedPremixCount;
    private long rejectedPremixCount;
    private long inProgressPremixCount;
    private long totalPremixCount;

    private long finalMixCount;
    private long pendingFinalMixCount;
    private long approvedFinalMixCount;
    private long rejectedFinalMixCount;
    private long inProgressFinalMixCount;
    private long totalFinalMixCount;

    // Status Lists
    private List<PremixStatusResponse> premixStatuses;
    private List<PremixStatusResponse> finalMixStatuses;

    // Audit Fields (Standardized to Object/String or PersonInfo/LocalDateTime
    // depending on your preference. Using PersonInfo and LocalDateTime is usually cleaner)
    private PersonInfo submittedBy; // Or String, if you prefer
    private LocalDateTime submittedAt;

    private String lastUpdatedBy;
    private LocalDateTime lastUpdatedAt;
}
