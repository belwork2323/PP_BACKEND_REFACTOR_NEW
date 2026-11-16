package in.co.bel.acempp.dto.rawmaterialpreparation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApCoarseProcessDto {
    /** Equipment type label, e.g. RVD. */
    private String equipmentType;

    @Builder.Default
    private List<OperationParameterRowDto> blendingDryingParameters = new ArrayList<>();

    @Builder.Default
    private List<TimedOperationParameterRowDto> dryingOperationRvd = new ArrayList<>();

    @Builder.Default
    private List<ParticleSizeRowDto> particleSizeDistribution = new ArrayList<>();

    /** QA Raw Material Processing only — ignored by manufacturing RMP UI. */
    private String blendingDryingQaRemarks;
    private String dryingOperationRvdQaRemarks;
    private String particleSizeDistributionQaRemarks;
}
