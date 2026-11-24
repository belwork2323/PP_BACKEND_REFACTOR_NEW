package in.co.bel.acempp.dto.trimming;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.co.bel.acempp.dto.common.BatchFormRequest;
import in.co.bel.acempp.dto.trimming.trimmingcreation.MotorDTO;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.validation.groups.CreateGroup;
import in.co.bel.acempp.validation.groups.DetailGroup;
import in.co.bel.acempp.validation.groups.UpdateGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrimmingRequest extends BatchFormRequest {
    @Valid
    @NotEmpty(message = "Motors list cannot be empty", groups = {CreateGroup.class, UpdateGroup.class})
    private List<MotorDTO> motors;
}
