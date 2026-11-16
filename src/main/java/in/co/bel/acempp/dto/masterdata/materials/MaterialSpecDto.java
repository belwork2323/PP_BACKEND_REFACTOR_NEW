package in.co.bel.acempp.dto.masterdata.materials;

import in.co.bel.acempp.dto.rawmaterialsourcing.ReferenceRange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialSpecDto {
    private String specificationCode;
    private String specificationName;
    private ReferenceRange referenceRange;
    private Boolean isActive;
}
