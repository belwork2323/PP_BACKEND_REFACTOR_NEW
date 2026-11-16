package in.co.bel.acempp.dto.dashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DepartmentProgressDto {
    private String department;
    private long batchCount;
    private int percentage;
}