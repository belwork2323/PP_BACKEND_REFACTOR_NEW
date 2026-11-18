package in.co.bel.acempp.dto.mixing.mixingform;

import in.co.bel.acempp.dto.common.BatchFormRequest;
import in.co.bel.acempp.dto.mixing.mixingcycle.MixingDetailsDto;
import in.co.bel.acempp.validation.groups.CreateGroup;
import in.co.bel.acempp.validation.groups.UpdateGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MixingFormRequest extends BatchFormRequest {

    @Valid
    @NotNull(message = "Mixing details are required",
            groups = {CreateGroup.class, UpdateGroup.class})
    private MixingDetailsDto mixingDetails;

}

