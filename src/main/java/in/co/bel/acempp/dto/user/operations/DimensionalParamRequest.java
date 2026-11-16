package in.co.bel.acempp.dto.user.operations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
public class DimensionalParamRequest {
    @NotBlank
    private String projectId;

    @NotNull
    private Integer motorType;
}
