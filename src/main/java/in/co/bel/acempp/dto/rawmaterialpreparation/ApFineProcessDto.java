package in.co.bel.acempp.dto.rawmaterialpreparation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApFineProcessDto {
    /** Grinding (ACM) — lot details replace multi-batch / coarse lot-qty rows. */
    private String acmEquipmentId;
    private String millRpm;
    private String classifierRpm;
    private String screwFeederRpm;
    private String idFanRpm;
    private String setPressure;
    private LocalDateTime grindingStartDatetime;
    private LocalDateTime grindingEndDatetime;
    private String grindingObservation;
    /** QA Raw Material Processing only — ignored by manufacturing RMP UI. */
    private String grindingQaRemarks;

    @Builder.Default
    private List<ParticleSizeRowDto> particleSizeDistribution = new ArrayList<>();

    private String qtyKgQualified;

    @Builder.Default
    private List<OperationParameterRowDto> blendingDryingParameters = new ArrayList<>();

    /** QA Raw Material Processing only. */
    private String blendingDryingQaRemarks;

    @Builder.Default
    private List<TimedOperationParameterRowDto> dryingOperationRvd = new ArrayList<>();

    /** QA Raw Material Processing only. */
    private String dryingOperationRvdQaRemarks;

    private DryingTrayOvenDto trayOvenStorage;
    private SievingDto sieving;
}
