package in.co.bel.acempp.dto.masterdata.qcdivisions;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QcDivisionMasterDeleteRequest {
    @NotBlank
    private String id;
}
