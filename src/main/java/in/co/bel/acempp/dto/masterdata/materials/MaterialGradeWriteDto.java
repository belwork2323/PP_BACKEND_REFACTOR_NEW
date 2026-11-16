package in.co.bel.acempp.dto.masterdata.materials;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
public class MaterialGradeWriteDto {
    private String gradeId;
    @NotBlank
    private String gradeCode;
    @NotBlank
    private String gradeName;
    @Valid
    @Builder.Default
    private List<MaterialSpecWriteDto> specifications = new ArrayList<>();
}
