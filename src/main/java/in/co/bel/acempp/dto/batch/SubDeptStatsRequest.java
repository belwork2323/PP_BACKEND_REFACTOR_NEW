package in.co.bel.acempp.dto.batch;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubDeptStatsRequest {
    @NotNull
    private Long subDepartmentId;
}
