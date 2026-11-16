package in.co.bel.acempp.dto.batch;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.general.ProjectDto;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.entity.batch.Batch;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BatchSummaryDto {
    private String id;
    private String batchId;
    private List<String> motorIds;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> bemMotorIds;
    private Integer motorStage;
    private List<String> lotIds;
    private ProjectDto project;
    private String batchType;               // "Main Batch" or "Subscale Batch"
    private Boolean parallelFlowEnabled;
    private Boolean isBatchLocked;
    private String priority;
    private PersonInfo systemManager;
    private Long subDepartmentId;
    private Status status;
    private List<Batch.StageProgress> currentStage;
    private List<Batch.StageProgress> stageProgress;
    private String identificationSheetStatus;
    private LocalDateTime createdOn;
    private PersonInfo createdBy;
}
