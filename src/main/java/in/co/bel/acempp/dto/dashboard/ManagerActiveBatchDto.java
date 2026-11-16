package in.co.bel.acempp.dto.dashboard;

//import in.co.bel.acempp.DTO.Batch.BatchStageDto;
//import in.co.bel.acempp.dto.Batch.MotorTypeDto;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

// ManagerActiveBatchDto.java
@Data
@Builder
public class ManagerActiveBatchDto {
    private String         batchId;
    private String         projectName;
    private Long         motorId;
//    private MotorTypeDto motorType;
//    private BatchStageDto stage;
    private String         priority;
    private String         status;
    private int            progressPercentage;
    private LocalDateTime  createdDate;
    private LocalDateTime  lastUpdatedOn;
}