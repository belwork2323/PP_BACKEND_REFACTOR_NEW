package in.co.bel.acempp.dto.rawmaterialpreparation;

import lombok.Data;

/**
 * Per-premix weighment header — mixer building and deviation/validation are not shared across premixes.
 */
@Data
public class WeightmentPremixSheetRequest {
    private Integer premixNo;
    private String mixerBuildingNumber;
    private ValidationRequest validation;
}
