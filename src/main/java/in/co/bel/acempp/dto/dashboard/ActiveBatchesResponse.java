package in.co.bel.acempp.dto.dashboard;

import in.co.bel.acempp.dto.general.PaginationDto;
import lombok.*;

import java.util.List;

@Data @Builder
public class ActiveBatchesResponse {
    private List<ActiveBatchItemDto> batches;
    private PaginationDto pagination;
}