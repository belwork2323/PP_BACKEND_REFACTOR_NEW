package in.co.bel.acempp.dto.rawmaterialsourcing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RmpFormDetailsRequest {
    @NotBlank
    private String formId;
    @NotNull
    private Long   subDepartmentId;
}