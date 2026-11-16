package in.co.bel.acempp.dto.common;

import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.validation.groups.CreateGroup;
import in.co.bel.acempp.validation.groups.DetailGroup;
import in.co.bel.acempp.validation.groups.UpdateGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BatchFormRequest {
    @NotBlank(message = "Form Id is required", groups = {UpdateGroup.class , DetailGroup.class})
    private String formId;

    @NotBlank(message = "Batch Id is required", groups = {CreateGroup.class, UpdateGroup.class})
    private String batchId;

    @NotNull(message = "Sub Department Id is required", groups = CreateGroup.class)
    private Long subDepartmentId;

    @NotNull(message = "Form Submission Type is required", groups = {CreateGroup.class, UpdateGroup.class})
    private Status formSubmissionType;
}
