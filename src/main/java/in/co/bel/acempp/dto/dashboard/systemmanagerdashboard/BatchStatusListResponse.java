package in.co.bel.acempp.dto.dashboard.systemmanagerdashboard;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchStatusListResponse {

    private String batchId;

    private DepartmentInfoDto department;

    private Integer progressPercentage;

    private LocalDateTime createdOn;

    private LocalDateTime lastUpdatedOn;

    private BatchStageInfoDto stage;
}