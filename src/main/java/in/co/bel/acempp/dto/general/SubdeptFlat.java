package in.co.bel.acempp.dto.general;

import jakarta.validation.constraints.*;
import lombok.*;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubdeptFlat {

    @NotNull(message = "subDepartmentId is required")
    private Long subDepartmentId;

    @NotBlank(message = "subDepartmentName is required")
    private String subDepartmentName;

    @NotNull(message = "departmentId is required")
    private Long departmentId;
}
