package in.co.bel.acempp.dto.rawmaterialsourcing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SpecificationResponse {
    private String specificationCode;
    private String specificationName;
    private ReferenceRange referenceRange;
    private Boolean isOutOfRange;
    private String analysedResult;
    private String acemQcResult;
    private String status;
}
