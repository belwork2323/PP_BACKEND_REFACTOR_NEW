package in.co.bel.acempp.dto.batch.batchdetails;

import lombok.*;
import java.util.List;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchDeptStagesDto {
    private String batchId;
    private LocalDateTime createdOn;
    private Integer ageInDays;
    private Integer overallProgress;
    private List<DepartmentStageDto> stages;
}
