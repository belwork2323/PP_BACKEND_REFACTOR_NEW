package in.co.bel.acempp.dto.dashboard;

import in.co.bel.acempp.dto.general.PaginationDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BlockchainEventsResponse {
    private List<BlockchainEventDto> events;
    private PaginationDto pagination;
}