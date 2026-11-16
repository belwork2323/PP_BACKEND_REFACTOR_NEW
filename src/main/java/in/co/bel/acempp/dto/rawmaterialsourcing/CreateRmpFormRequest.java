package in.co.bel.acempp.dto.rawmaterialsourcing;

import in.co.bel.acempp.enums.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateRmpFormRequest {
    @NotNull(message = "subDepartmentId is required")
    private Long subDepartmentId;

    @NotNull(message = "submissionType is required")
    private Status submissionType; // DRAFT or SUBMIT

    @NotEmpty(message = "materials list cannot be empty")
    @Valid
    private List<MaterialRequest> materials;
}