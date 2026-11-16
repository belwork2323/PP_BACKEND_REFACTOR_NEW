package in.co.bel.acempp.dto.batch;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreateBatchRequest {
    @NotBlank
    private String batchType;               // MAIN / SUBSCALE

    private String subBatchType;            // QUALIFICATION / EXPERIMENTAL (only for SUBSCALE)

    private String projectId;               // nullable for EXPERIMENTAL

    private Integer motorStage;              // e.g. "B" (replaces motorType)

    private Integer numberOfMotors;

    private List<String> motorIds;          // required for MAIN / QUALIFICATION

    private List<SubscaleArticleRequest> articles;          // required for EXPERIMENTAL

    private String objective;               // required for EXPERIMENTAL

    @NotBlank
    private String priority;

    private String systemManagerId;

    @Valid
    private IdentificationSheetDto identificationSheet;   // optional
}