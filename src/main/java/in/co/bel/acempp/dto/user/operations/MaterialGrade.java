package in.co.bel.acempp.dto.user.operations;

import in.co.bel.acempp.entity.MaterialSpecification;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialGrade {

    private String gradeId;

    private String gradeCode;

    private String gradeName;

    private List<MaterialSpecification> specifications;
}