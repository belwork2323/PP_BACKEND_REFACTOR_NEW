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
public class BatchStatusItemDto {

    private String batchId;

    private String motorId;

    private String projectName;

    private BatchStageInfoDto stage;

    private Integer progressPercentage;

    private LocalDateTime createdDate;

    private LocalDateTime lastUpdatedOn;
}