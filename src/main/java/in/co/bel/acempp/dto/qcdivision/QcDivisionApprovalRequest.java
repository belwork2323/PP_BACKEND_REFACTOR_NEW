package in.co.bel.acempp.dto.qcdivision;

import in.co.bel.acempp.enums.StageType;
import in.co.bel.acempp.enums.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QcDivisionApprovalRequest {
    @NotBlank
    private String formId;
    @NotNull
    private Long divisionId;
    @NotBlank
    private String division;
    private String subType;
    private Integer premixNo;
    private String motorId;
    private StageType stageType;
    @NotNull
    private Long subDepartmentId;
    @NotNull
    private Status actionType;
    private String remarks;
    private String rejectionReason;
}
