package in.co.bel.acempp.dto.dashboard.systemmanagerdashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemManagerActiveBatchesResponse {

    private Long totalRecords;

    private Integer totalPages;

    private Integer currentPage;

    private Integer pageSize;

    private List<SystemManagerActiveBatchDto> batches;
}