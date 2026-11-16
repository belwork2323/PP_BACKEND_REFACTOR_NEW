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
public class DepartmentDto {

    private Long departmentId;

    private String departmentName;

    private List<SubDepartmentDto> subDepartment;
}