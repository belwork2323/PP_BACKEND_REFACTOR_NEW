package in.co.bel.acempp.dto.masterdata.materials;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialMasterListRequest {
    private String search;
    private Boolean isActive;
    private String materialType;
    private String rawMaterialType;
    private String preparationType;
}
