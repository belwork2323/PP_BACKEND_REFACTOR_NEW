package in.co.bel.acempp.dto.batch.batchdetails;

import lombok.*;

@Data
@Builder
public class WorkflowInsights {
    private String currentStatus;
    private String rejectionReason;
    private boolean approvalPending;
    private boolean reworkRequired;
    private int resubmissionCount;
}