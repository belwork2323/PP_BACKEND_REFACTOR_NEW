package in.co.bel.acempp.dto.batch;

import lombok.Data;

@Data
public class BatchListRequest {
//    @NotNull
    private Long subDepartmentId;
    private Integer page;        // ← Direct field
    private Integer limit;       // ← Direct field
    private BatchPaginationDto pagination;
    private BatchFiltersDto filters;
    private BatchSortDto sort;
}