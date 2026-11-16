package in.co.bel.acempp.dto.user.operations;

import lombok.*;
@Data
@Builder
public class DimensionalParamDto {
    private String               paramId;
    private String               paramName;
    private SpecificationRangeDto referenceRange; // reuse same range DTO
}
