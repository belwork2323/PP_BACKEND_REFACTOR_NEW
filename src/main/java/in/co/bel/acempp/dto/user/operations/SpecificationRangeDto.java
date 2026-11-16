package in.co.bel.acempp.dto.user.operations;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpecificationRangeDto {
    private Double minValue;
    private Double maxValue;
    private Long unitId;
    private String unit;
}
