package in.co.bel.acempp.entity.manufacturing;

import lombok.*;

import java.util.List;

@Data
@Builder @NoArgsConstructor @AllArgsConstructor
public class WeightmentSheet {
    /** Legacy shared mixer — prefer {@link #premixSheets} when present. */
    private String mixerBuildingNumber;
    private List<WeightmentDetail> weightmentDetails;
    /** Legacy shared validation — prefer {@link #premixSheets} when present. */
    private Validation validation;
    /** Per-premix mixer building + deviation/validation. */
    private List<WeightmentPremixSheet> premixSheets;
}
