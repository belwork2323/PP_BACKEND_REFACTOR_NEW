package in.co.bel.acempp.dto.batch;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.general.ProjectDto;
import in.co.bel.acempp.entity.batch.Batch;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BatchDetailDto {
    private String batchId;
    private String batchType;
    private Boolean parallelFlowEnabled;
    private String subBatchType;
    private ProjectDto project;
    private Integer motorStage;
    private Integer numberOfMotors;
    private List<String> motorIds;
    private List<SubscaleArticleInfo> articles;          // only for EXPERIMENTAL
    private String objective;               // only for EXPERIMENTAL
    private String priority;
    private PersonInfo systemManager;
    private IdentificationSheetDto identificationSheet;
    private List<Batch.StageProgress> currentStage;
    private List<Batch.StageProgress> stageProgress;
    private String identificationSheetStatus;
    private LocalDateTime createdOn;
    private PersonInfo createdBy;
    private LocalDateTime updatedOn;
    private PersonInfo updatedBy;
}
