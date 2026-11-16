package in.co.bel.acempp.dto.rawmaterialpreparation;

import lombok.Data;

import java.util.List;

@Data
public class WeightmentSheetRequest {
    /** Legacy shared mixer — prefer {@link #premixSheets} when present. */
    private String mixerBuildingNumber;
    private List<WeightmentDetailRequest> weightmentDetails;
    /** Legacy shared validation — prefer {@link #premixSheets} when present. */
    private ValidationRequest validation;
    /** Per-premix mixer building + deviation/validation. */
    private List<WeightmentPremixSheetRequest> premixSheets;
}