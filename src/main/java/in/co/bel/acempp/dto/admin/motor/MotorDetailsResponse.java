package in.co.bel.acempp.dto.admin.motor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotorDetailsResponse {
    private String motorId;
    private String motorCasingId;
    private String casingStatus;
    private String projectId;
    private Integer motorStage;
    /** DISCARDED / DISPATCHED / IN_PROGRESS (same lifecycle as Motor Management list). */
    private String workflowStatus;
    private List<MotorLinkedBatchDto> linkedBatches;
    private Boolean discarded;
    private String discardReason;
}
