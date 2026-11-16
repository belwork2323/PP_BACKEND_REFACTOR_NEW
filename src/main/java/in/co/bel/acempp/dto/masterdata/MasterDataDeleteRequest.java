package in.co.bel.acempp.dto.masterdata;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterDataDeleteRequest {

    @NotNull(message = "Id is required")
    private Long id;
}
