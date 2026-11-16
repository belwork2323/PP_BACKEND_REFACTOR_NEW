package in.co.bel.acempp.dto.rawmaterialsourcing;

import in.co.bel.acempp.dto.rawmaterialsourcing.preparation.AdductPreparationDetails;
import in.co.bel.acempp.dto.rawmaterialsourcing.preparation.HtpbBlendingPreparationDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LotRequest {
    @NotBlank(message = "lotId is required")
    private String lotId;

    @Valid
    private List<SpecificationRequest> specifications;

    @Valid
    private List<CertificateRequest> certificates;

    @Valid
    private AdductPreparationDetails adductPreparation;

    @Valid
    private HtpbBlendingPreparationDetails htpbBlendingPreparation;
}
