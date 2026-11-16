package in.co.bel.acempp.dto.masterdata.insulation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsulationSpecListRequest {
    private String search;
    private Boolean isActive;
}
