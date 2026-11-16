package in.co.bel.acempp.dto.masterdata.insulation;

import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.CategoryDto;
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
public class InsulationSpecCreateRequest {
    @NotBlank
    private String insulationType;

    private Boolean isActive;

    @Valid
    @Builder.Default
    private List<CategoryDto> specifications = new ArrayList<>();
}
