package in.co.bel.acempp.dto.rawmaterialpreparation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApUltraFineProcessDto {
    /** Grinding — lot details replace multi-batch / coarse lot-qty rows. */
    private String equipmentId;
    private String screwFeederRpm;
    private String feedPressure;
    private String grindingPressure;
    private LocalDateTime grindingStartDatetime;
    private LocalDateTime grindingEndDatetime;
    private String grindingObservation;
    /** QA Raw Material Processing only — ignored by manufacturing RMP UI. */
    private String grindingQaRemarks;
    /** Spec is fixed 6±1μm; this holds the measured result. */
    private String particleSizeResult;
    private String qtyKgQualified;

    private DryingTrayOvenDto drying;
    private SievingDto sieving;
}
