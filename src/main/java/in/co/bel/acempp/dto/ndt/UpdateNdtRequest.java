package in.co.bel.acempp.dto.ndt;

import in.co.bel.acempp.enums.Status;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNdtRequest {
    @NotBlank
    private String formId;
    @NotBlank
    private String batchId;
    @NotNull
    private Long subDepartmentId;
    @NotNull
    private Status formSubmissionType;

    private List<NdtMotorDto> motors;
}