package in.co.bel.acempp.dto.statictestingfacility;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.common.BatchFormRequest;
import in.co.bel.acempp.enums.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class CreateStfRequest extends BatchFormRequest {
    @NotEmpty
    @JsonDeserialize(using = StfMotorListDeserializer.class)
    private List<@Valid StfMotorRequest> motors;
}
