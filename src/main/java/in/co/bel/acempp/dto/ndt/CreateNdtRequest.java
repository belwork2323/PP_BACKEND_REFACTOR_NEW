package in.co.bel.acempp.dto.ndt;

import in.co.bel.acempp.dto.common.BatchFormRequest;
import in.co.bel.acempp.enums.Status;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class CreateNdtRequest extends BatchFormRequest {
    @NotEmpty
    private List<NdtMotorDto> motors;
}