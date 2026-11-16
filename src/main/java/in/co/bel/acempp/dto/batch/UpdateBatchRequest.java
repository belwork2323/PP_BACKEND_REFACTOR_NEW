package in.co.bel.acempp.dto.batch;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBatchRequest {
    @NotBlank
    private String batchId;
    @NotBlank
    private String batchType;
    private String subBatchType;
    private String projectId;
    private String objective;

    private Integer motorStage;

    private Integer numberOfMotors;
    private List<String> motorIds;
    private List<SubscaleArticleRequest> articles;
    @NotBlank
    private String priority;
    @NotBlank
    private String systemManagerId;

    @Valid
    private IdentificationSheetDto identificationSheet;   // null = don't touch
}