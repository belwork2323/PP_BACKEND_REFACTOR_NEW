package in.co.bel.acempp.dto.castingcuring;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.common.BatchFormRequest;
import in.co.bel.acempp.validation.groups.CreateGroup;
import in.co.bel.acempp.validation.groups.UpdateGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CastingCuringRequest  extends BatchFormRequest {
    @NotEmpty(message = "motors cannot be empty",
            groups = {CreateGroup.class, UpdateGroup.class})
    @JsonDeserialize(using = CastingCuringMotorListDeserializer.class)
    private List<@NotNull(message = "motor is required",
            groups = {CreateGroup.class, UpdateGroup.class}) @Valid CastingCuringMotor> motors;

}
