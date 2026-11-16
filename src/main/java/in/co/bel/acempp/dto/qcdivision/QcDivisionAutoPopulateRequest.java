package in.co.bel.acempp.dto.qcdivision;

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
public class QcDivisionAutoPopulateRequest {

    @NotBlank(message = "batchId is required")
    private String batchId;

    @NotNull(message = "divisionId is required")
    private Long divisionId;
}
