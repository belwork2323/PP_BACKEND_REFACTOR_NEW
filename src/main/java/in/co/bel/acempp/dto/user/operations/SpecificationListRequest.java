package in.co.bel.acempp.dto.user.operations;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SpecificationListRequest {
    @NotBlank
    private String materialCode;

    private String gradeCode;
}