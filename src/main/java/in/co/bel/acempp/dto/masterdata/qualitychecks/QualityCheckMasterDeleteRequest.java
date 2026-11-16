package in.co.bel.acempp.dto.masterdata.qualitychecks;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityCheckMasterDeleteRequest {
    @NotNull
    private Long id;
}
