package in.co.bel.acempp.dto.user.operations;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.general.ProjectDto;
import lombok.Builder;
import lombok.Data;
import in.co.bel.acempp.entity.batch.Batch;

import java.time.LocalDateTime;
import java.util.List;

// UserBatchItemDto.java
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserBatchItemDto {
    private String id;
    private String batchId;
    private String formId;
    private List<String> motorIds;
    private List<String> bemMotorIds;
    private Integer motorStage;
    private List<String> lotIds;
    private ProjectDto project;
    private String batchType;
    private Boolean parallelFlowEnabled;
    private String priority;
    private PersonInfo formSubmittedBy;
    private PersonInfo systemManager;
    private Long subDepartmentId;
    private List<Batch.StageProgress> currentStage;
    private List<Batch.StageProgress> stageProgress;
    private String identificationSheetStatus;
    private LocalDateTime createdOn;
    private PersonInfo createdBy;
    private LocalDateTime submittedAt;
    private String rejectionReason;
}
