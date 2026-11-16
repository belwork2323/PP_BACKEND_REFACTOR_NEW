package in.co.bel.acempp.dto.rawmaterialpreparation;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PreparationDetailsRequest {
    private List<PremixRequest> premixes;
    private WeightmentSheetRequest weightmentSheet;
}
