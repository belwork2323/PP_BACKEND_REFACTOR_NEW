package in.co.bel.acempp.dto.casepreparation;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.enums.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCasePreparationRequest {
    @NotBlank
    private String formId;

    private String batchId;               // optional, can be ignored or validated

    @NotBlank
    private String batchType;

    @NotNull
    private Long subDepartmentId;

    @NotNull
    private Status formSubmissionType;    // DRAFT / FINAL_SUBMIT

    @Valid
    @JsonDeserialize(using = CasePreparationDetailsRequestDeserializer.class)
    private CasePreparationDetailsRequest casePreparationDetails;

    @JsonAnySetter
    public void rejectUnknownField(String field, Object ignored) {
        throw new IllegalArgumentException("Unknown Case Preparation update field: " + field);
    }
}
