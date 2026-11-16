package in.co.bel.acempp.dto.user.operations;

import in.co.bel.acempp.dto.general.PaginationDto;
import in.co.bel.acempp.dto.general.StatusCountsDto;
import lombok.*;
import java.util.List;

@Data
@Builder
public class UserBatchListResponse {
    private StatusCountsDto statusCounts;
    private List<UserBatchItemDto> batches;
    private PaginationDto pagination; // reuse existing
}