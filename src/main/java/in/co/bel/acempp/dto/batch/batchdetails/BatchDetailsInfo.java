package in.co.bel.acempp.dto.batch.batchdetails;

import in.co.bel.acempp.enums.Status;
import lombok.*;

@Data
@Builder
public class BatchDetailsInfo {
    private String batchId;
    private String batchType;
    private String projectId;
    private String subDepartmentStage;
    private Status submissionType;
    private String mockTrialStatus;
    private String processType;
}