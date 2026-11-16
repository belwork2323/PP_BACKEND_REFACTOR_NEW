package in.co.bel.acempp.entity.manufacturing;

import lombok.*;

/**
 * Per-premix weighment header (mixer building + deviation/validation).
 * Detail rows remain on {@link WeightmentSheet#getWeightmentDetails()} with {@code premixNo}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeightmentPremixSheet {
    private Integer premixNo;
    private String mixerBuildingNumber;
    private Validation validation;
}
