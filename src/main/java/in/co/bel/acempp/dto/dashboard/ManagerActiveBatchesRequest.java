package in.co.bel.acempp.dto.dashboard;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ManagerActiveBatchesRequest {
    @NotNull
    private int    page;
    @NotNull private int    limit;
    private String  search;
    private Long    departmentId;
    private Long    subDepartmentId;
    private String  priority;
    private String  status;
//    private Sort sort;
}