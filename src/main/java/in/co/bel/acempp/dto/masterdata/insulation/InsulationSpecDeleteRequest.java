package in.co.bel.acempp.dto.masterdata.insulation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsulationSpecDeleteRequest {
    @NotNull
    private Long insulationSpecId;
}
