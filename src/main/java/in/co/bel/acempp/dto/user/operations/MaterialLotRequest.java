package in.co.bel.acempp.dto.user.operations;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MaterialLotRequest {
    @NotBlank
    private String batchId;
}