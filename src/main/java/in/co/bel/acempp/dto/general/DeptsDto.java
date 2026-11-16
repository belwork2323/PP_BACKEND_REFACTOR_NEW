package in.co.bel.acempp.dto.general;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeptsDto {
    private Long departmentId;
    private String departmentName;
    private List<SubDeptsDto> subDepartments;

}
