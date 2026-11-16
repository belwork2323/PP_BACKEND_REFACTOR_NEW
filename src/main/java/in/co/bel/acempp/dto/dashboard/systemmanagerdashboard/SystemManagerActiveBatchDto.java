package in.co.bel.acempp.dto.dashboard.systemmanagerdashboard;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import in.co.bel.acempp.entity.batch.Batch;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemManagerActiveBatchDto {

    private String batchId;

    private String batchType;

    private String projectName;

    private String motorId;

    private MotorTypeDto motorType;

    private List<Batch.StageProgress> currentStage;

    private List<Batch.StageProgress> stageProgress;

    private String priority;

    private Integer progressPercentage;

    private LocalDateTime createdDate;

    private LocalDateTime lastUpdatedOn;
}
