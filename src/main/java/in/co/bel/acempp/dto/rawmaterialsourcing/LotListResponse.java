package in.co.bel.acempp.dto.rawmaterialsourcing;

import in.co.bel.acempp.dto.general.*;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LotListResponse {
    private StatusCountsDto statusCounts;
    private List<LotSummary> lots;
    private PaginationDto pagination;
}
