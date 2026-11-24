package in.co.bel.acempp.dto.casepreparation;

import in.co.bel.acempp.enums.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MotorApprovalRequest {

    @NotBlank
    private String formId;

    @NotBlank
    private String motorId;

    private Long divisionId;

    private String division;

    private String subType;

    @NotNull
    private Long subDepartmentId;

    @NotNull
    private Status actionType;

    @Pattern(
            regexp = "^$|^[A-Za-z0-9\\s@$%^&*()=\\-\\+\\[\\]\\/\\\\\"'.,:<>]{3,500}$",
            message = "Remarks must be between 10 and 500 characters and contain only allowed characters (@$%^&*()-+[]/\\\"'.,:<>)"
    )
    private String remarks;

    @Pattern(
            regexp = "^$|^[A-Za-z0-9\\s@$%^&*()=\\-\\+\\[\\]\\/\\\\\"'.,:<>]{3,500}$",
            message = "Rejection reason must be between 3 and 500 characters and contain only allowed characters (@$%^&*()-+[]/\\\"'.,:<>)"
    )
    private String rejectionReason;
}
