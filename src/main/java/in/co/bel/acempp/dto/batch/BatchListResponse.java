package in.co.bel.acempp.dto.batch;

import in.co.bel.acempp.dto.general.PaginationDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BatchListResponse {
    private List<BatchSummaryDto> batches;
    private PaginationDto pagination;
}