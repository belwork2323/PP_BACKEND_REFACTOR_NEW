package in.co.bel.acempp.dto.masterdata.qualitychecks;

import in.co.bel.acempp.dto.mixing.qualitychecks.QualityCheckParams;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class QualityCheckMasterCreateRequest {
    @NotBlank
    private String mixType;

    @NotNull
    private Integer motorStage;

    private Boolean isActive;

    @Valid
    @Builder.Default
    private List<QualityCheckParams> qualityChecks = new ArrayList<>();
}
