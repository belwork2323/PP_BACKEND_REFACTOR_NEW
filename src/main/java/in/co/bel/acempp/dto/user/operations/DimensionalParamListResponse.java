package in.co.bel.acempp.dto.user.operations;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DimensionalParamListResponse {
    private Integer                   motorType;
    private List<DimensionalParamDto> parameters;
}
