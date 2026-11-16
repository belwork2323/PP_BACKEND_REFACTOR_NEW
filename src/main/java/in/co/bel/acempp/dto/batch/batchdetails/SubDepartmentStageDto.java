package in.co.bel.acempp.dto.batch.batchdetails;

import lombok.*;

@Data
@Builder
public class SubDepartmentStageDto {
    private Long subDepartmentId;
    private String name;
    private String status;               // e.g. Approved, Waiting for Approval, Rejected, Not Started
    private int completionPercentage;
    private String riskLevel;
    private boolean isDelayed;
    private boolean hasFailures;
    private boolean approvalPending;
}