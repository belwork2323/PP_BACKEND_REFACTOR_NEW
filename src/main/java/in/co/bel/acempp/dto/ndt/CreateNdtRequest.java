package in.co.bel.acempp.dto.ndt;

import in.co.bel.acempp.enums.Status;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.*;

@Data
@AllArgsConstructor
public class CreateNdtRequest {
    @NotBlank
    private String batchId;
    @NotNull
    private Long subDepartmentId;
    @NotBlank
    private Status formSubmissionType;
    @NotEmpty
    private List<NdtMotorDto> motors;
//    private List<Map<String, Object>> motors;
}