package in.co.bel.acempp.dto.admin.motor;

import in.co.bel.acempp.dto.general.PersonInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotorSummaryDto {
    private String motorId;
    private List<String> batchIds;
    private Integer linkedBatchCount;
    private List<MotorLinkedBatchDto> linkedBatches;
    private String primaryBatchId;
    private String projectId;
    private Integer motorStage;
    private String batchType;
    private String workflowStatus;
    private String workflowSubmissionType;
    private String motorCasingId;
    private String casingStatus;
    private Boolean discarded;
    private String discardReason;
    /** From rocket motor casing registration. */
    private PersonInfo createdBy;
    private LocalDateTime createdAt;
}
