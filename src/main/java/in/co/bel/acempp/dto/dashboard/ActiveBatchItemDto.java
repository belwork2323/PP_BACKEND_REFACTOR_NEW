package in.co.bel.acempp.dto.dashboard;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.enums.Status;
import lombok.*;
import java.util.List;

@Data
@Builder
public class ActiveBatchItemDto {
    private String batchId;
    private String batchType;
    private String motorId;          // first motor ID
    private String projectName;
    private List<Batch.StageProgress> currentStage;
    private List<Batch.StageProgress> stageProgress;
    private PersonInfo systemManager;
    private String date;             // yyyy-MM-dd
    private int completion;
}
