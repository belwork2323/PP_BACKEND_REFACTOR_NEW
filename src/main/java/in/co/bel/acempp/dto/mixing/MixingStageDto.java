package in.co.bel.acempp.dto.mixing;

import in.co.bel.acempp.dto.mixing.mixingform.MixingFormRequest;
import in.co.bel.acempp.dto.mixing.mixingform.PremixDto;
import in.co.bel.acempp.enums.StageType;
import in.co.bel.acempp.validation.groups.CreateGroup;
import in.co.bel.acempp.validation.groups.UpdateGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@Builder
public class MixingStageDto {

    @NotNull(message = "Stage Type is required", groups = {CreateGroup.class, UpdateGroup.class})
    private StageType stageType;

    @NotEmpty(message = "At least one premix entry is required", groups = {CreateGroup.class, UpdateGroup.class})
    private List<
            @NotNull(message = "Premix entry is required", groups = {CreateGroup.class, UpdateGroup.class})
            @Valid
                    PremixDto
            > premixes;
}
