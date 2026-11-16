package in.co.bel.acempp.dto.mixing.mixingcycle;

import in.co.bel.acempp.dto.mixing.MixingStageDto;
import in.co.bel.acempp.validation.groups.CreateGroup;
import in.co.bel.acempp.validation.groups.UpdateGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MixingDetailsDto {

    @NotEmpty(message = "At least one stage is required", groups = {CreateGroup.class, UpdateGroup.class})
    private List<@NotNull(message = "Stage is required", groups = {CreateGroup.class, UpdateGroup.class})
    @Valid MixingStageDto> stages;
}
