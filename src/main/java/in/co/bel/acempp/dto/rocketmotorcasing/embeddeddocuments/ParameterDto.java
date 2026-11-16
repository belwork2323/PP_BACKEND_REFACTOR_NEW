package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;

import in.co.bel.acempp.dto.rawmaterialsourcing.ReferenceRange;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterDto {

    private String specificationCode;

    private String specificationName;

    private ReferenceRange referenceRange;

    private Boolean isActive;
}