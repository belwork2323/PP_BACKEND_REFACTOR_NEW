package in.co.bel.acempp.entity;

import in.co.bel.acempp.dto.rawmaterialsourcing.ReferenceRange;
import lombok.*;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialSpecification {
    private String specificationCode;
    private String specificationName;
    private ReferenceRange referenceRange;
    /** Null treated as active for documents created before soft-disable. */
    private Boolean isActive;
}
