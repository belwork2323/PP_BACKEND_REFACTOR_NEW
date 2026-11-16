package in.co.bel.acempp.dto.admin.motor;

import in.co.bel.acempp.dto.general.PersonInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotorLinkedBatchDto {
    private String batchId;
    private String batchType;
    private String projectId;
    private Integer motorStage;
    private String workflowStatus;
    private String workflowSubmissionType;
    private LocalDateTime createdAt;
    private PersonInfo createdBy;
}
