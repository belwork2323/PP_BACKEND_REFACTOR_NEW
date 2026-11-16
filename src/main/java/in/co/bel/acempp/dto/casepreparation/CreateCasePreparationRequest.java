package in.co.bel.acempp.dto.casepreparation;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.enums.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCasePreparationRequest {

    @NotBlank
    private String batchId;

    @NotBlank
    private String batchType;

    @NotNull
    private Long subDepartmentId;

    @NotNull
    private Status formSubmissionType;

    @Valid
    @JsonDeserialize(using = CasePreparationDetailsRequestDeserializer.class)
    private CasePreparationDetailsRequest casePreparationDetails;

}
