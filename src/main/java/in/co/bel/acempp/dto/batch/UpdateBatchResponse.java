package in.co.bel.acempp.dto.batch;
import in.co.bel.acempp.entity.batch.Batch;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBatchResponse {
    private String batchId;
    private String batchType;
    private String subBatchType;
    private String projectId;
    private String priority;
    private String systemManagerId;
    private String identificationSheetStatus;
    private List<Batch.StageProgress> currentStage;
    private LocalDateTime updatedOn;
}
