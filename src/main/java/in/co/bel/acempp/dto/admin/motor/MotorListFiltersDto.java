package in.co.bel.acempp.dto.admin.motor;

import lombok.Data;

@Data
public class MotorListFiltersDto {
    private String search;
    private String batchId;
    private String projectId;
    private Integer motorStage;
    private String workflowStatus;
    private String filterType;
    private String startDate;
    private String endDate;
}
