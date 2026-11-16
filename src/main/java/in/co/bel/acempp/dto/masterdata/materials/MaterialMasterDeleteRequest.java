package in.co.bel.acempp.dto.masterdata.materials;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialMasterDeleteRequest {
    @NotNull
    private Long materialId;
}
