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
public class MaterialSpecWriteDto {
    private String specificationCode;
    /**
     * Required for new specifications. Optional on update when {@code specificationCode}
     * already exists — the service keeps the stored name and only applies {@code isActive}.
     */
    private String specificationName;
    private ReferenceRange referenceRange;
    private Boolean isActive;
}
