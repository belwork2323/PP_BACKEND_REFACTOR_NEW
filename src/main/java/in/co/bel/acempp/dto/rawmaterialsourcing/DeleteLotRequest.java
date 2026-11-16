package in.co.bel.acempp.dto.rawmaterialsourcing;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteLotRequest {
    @NotBlank(message = "lotId is required")
    private String lotId;
}