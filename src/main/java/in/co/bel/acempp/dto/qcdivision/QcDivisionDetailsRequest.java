package in.co.bel.acempp.dto.qcdivision;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QcDivisionDetailsRequest {
    @NotBlank
    private String formId;

    @NotNull
    private Long subDepartmentId;
}
