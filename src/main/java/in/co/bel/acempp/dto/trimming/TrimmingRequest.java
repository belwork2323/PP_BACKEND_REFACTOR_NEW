package in.co.bel.acempp.dto.trimming;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.co.bel.acempp.dto.trimming.trimmingcreation.MotorDTO;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.validation.groups.CreateGroup;
import in.co.bel.acempp.validation.groups.DetailGroup;
import in.co.bel.acempp.validation.groups.UpdateGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrimmingRequest {

    // Required for Update and Details operations
    @NotBlank(message = "Form ID is required", groups = {UpdateGroup.class, DetailGroup.class})
    private String formId;

    // Required only for Create operations
    @NotBlank(message = "Batch ID is required", groups = {CreateGroup.class})
    private String batchId;

    // Required for Create and Update
    @NotNull(message = "Sub Department ID is required", groups = {CreateGroup.class, UpdateGroup.class})
    private Long subDepartmentId;

    // Required for Create and Update
    @NotNull(message = "Form Submission Type is required", groups = {CreateGroup.class, UpdateGroup.class})
    private Status formSubmissionType;

    @Valid
    @NotEmpty(message = "Motors list cannot be empty", groups = {CreateGroup.class, UpdateGroup.class})
    private List<MotorDTO> motors;
}
