package in.co.bel.acempp.dto.rawmaterialsourcing;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LotDetailsRequest {
    @NotBlank(message = "lotId is required")
    private String lotId;
}
