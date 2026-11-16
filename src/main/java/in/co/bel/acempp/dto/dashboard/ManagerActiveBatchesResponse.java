package in.co.bel.acempp.dto.dashboard;

import lombok.Builder;
import lombok.Data;

import java.util.List;


@Data
@Builder
public class ManagerActiveBatchesResponse {
    private long                       totalRecords;
    private int                        totalPages;
    private int                        currentPage;
    private int                        pageSize;
    private List<ManagerActiveBatchDto> batches;
}