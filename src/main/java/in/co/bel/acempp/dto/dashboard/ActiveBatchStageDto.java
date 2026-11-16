package in.co.bel.acempp.dto.dashboard;

import lombok.*;

@Data
@Builder
public class ActiveBatchStageDto {
    private String department;
    private String subDepartment;
}