package in.co.bel.acempp.dto.dashboard.systemmanagerdashboard;

import in.co.bel.acempp.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerActiveBatchesRequest {

    private Integer page;

    private Integer limit;

    private String search;

    private String filterType;

    private String startDate;

    private String endDate;

    private Long departmentId;

    private Long subDepartmentId;

    private String priority;

    private Status status;

    private String listType;
}
