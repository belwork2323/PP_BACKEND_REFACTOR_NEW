package in.co.bel.acempp.dto.masterdata.insulation;

import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.CategoryDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
public class InsulationSpecUpdateRequest {
    @NotNull
    private Long insulationSpecId;

    private String insulationType;

    @NotNull
    private Boolean isActive;

    @Valid
    @Builder.Default
    private List<CategoryDto> specifications = new ArrayList<>();
}
