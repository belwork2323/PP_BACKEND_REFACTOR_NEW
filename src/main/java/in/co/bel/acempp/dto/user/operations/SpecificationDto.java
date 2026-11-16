package in.co.bel.acempp.dto.user.operations;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecificationDto {
    private String               specificationCode;
    private String               specificationName;
    private SpecificationRangeDto referenceRange;

}