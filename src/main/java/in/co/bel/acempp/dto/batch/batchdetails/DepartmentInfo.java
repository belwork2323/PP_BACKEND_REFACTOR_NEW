package in.co.bel.acempp.dto.batch.batchdetails;

import lombok.*;

@Data
@Builder
public class DepartmentInfo {
    private Long departmentId;
    private String departmentName;
}