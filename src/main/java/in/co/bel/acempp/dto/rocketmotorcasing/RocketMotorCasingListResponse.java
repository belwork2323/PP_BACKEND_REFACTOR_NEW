package in.co.bel.acempp.dto.rocketmotorcasing;
import in.co.bel.acempp.dto.general.PaginationDto;
import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.StatusCounts;
import lombok.*;

import java.util.List;

@Data
@Builder
public class RocketMotorCasingListResponse {

    private StatusCounts statusCounts;

    private List<CasingResponse> casings;

    private PaginationDto pagination;
}
