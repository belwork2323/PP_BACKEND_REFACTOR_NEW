package in.co.bel.acempp.dto.batch.batchdetails;

import lombok.*;

@Data
@Builder
public class DepartmentStageDto {
    private Long departmentId;
    private String departmentName;
    private String status;                 // NOT_STARTED / IN_PROGRESS / COMPLETED
    private Integer completionPercentage;
}
