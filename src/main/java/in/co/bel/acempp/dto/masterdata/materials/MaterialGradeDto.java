package in.co.bel.acempp.dto.masterdata.materials;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialGradeDto {
    private String gradeId;
    private String gradeCode;
    private String gradeName;
    @Builder.Default
    private List<MaterialSpecDto> specifications = new ArrayList<>();
}
