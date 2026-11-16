package in.co.bel.acempp.dto.rawmaterialsourcing;

import in.co.bel.acempp.dto.rawmaterialsourcing.preparation.AdductPreparationDetails;
import in.co.bel.acempp.dto.rawmaterialsourcing.preparation.HtpbBlendingPreparationDetails;
import in.co.bel.acempp.enums.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class UpdateLotRequest {

    @NotBlank(message = "lotId is required")
    private String lotId;

    @NotNull(message = "submissionType is required")
    private Status submissionType; // DRAFT or UPDATE

    @NotNull(message = "subDepartmentId is required")
    private Long subDepartmentId;

    private String supplyOrderNo;

    private LocalDate receiptDate;

    @NotBlank(message = "manufacturerName is required")
    private String manufacturerName;

    @NotBlank(message = "materialCode is required")
    private String materialCode;

    private String grade;

    @Valid
    private List<SpecificationRequest> specifications;

    @Valid
    private List<CertificateRequest> certificates;

    @Valid
    private AdductPreparationDetails adductPreparation;

    @Valid
    private HtpbBlendingPreparationDetails htpbBlendingPreparation;
}
