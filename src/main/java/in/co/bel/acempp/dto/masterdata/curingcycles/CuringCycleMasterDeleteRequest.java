package in.co.bel.acempp.dto.masterdata.curingcycles;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuringCycleMasterDeleteRequest {
    @NotNull
    private Long id;
}
