package in.co.bel.acempp.dto.rawmaterialpreparation;

import in.co.bel.acempp.enums.StageType;
import in.co.bel.acempp.enums.Status;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class PremixApprovalRequest {
    @NotBlank
    private String formId;

    @NotNull
    private Integer premixNo;

    private Long divisionId;

    private String division;

    private String subType;

    private StageType stageType;

    @NotNull
    private Long subDepartmentId;

    @NotNull
    private Status actionType;

    private String remarks;

    private String rejectionReason;
}
