package in.co.bel.acempp.dto.rawmaterialsourcing;

import in.co.bel.acempp.dto.rawmaterialsourcing.preparation.AdductPreparationDetails;
import in.co.bel.acempp.dto.rawmaterialsourcing.preparation.HtpbBlendingPreparationDetails;
import in.co.bel.acempp.dto.user.operations.MaterialGrade;
import in.co.bel.acempp.enums.PreparationType;
import in.co.bel.acempp.enums.RawMaterialCategory;
import in.co.bel.acempp.enums.Status;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class LotDetailsResponse {

    private String lotId;
    private Status submissionType;
    private Long subDepartmentId;
    private String supplyOrderNo;
    private LocalDate receiptDate;
    private String manufacturerName;
    private String materialCode;
    private MaterialGrade grade;
    private RawMaterialCategory rawMaterialType;
    private PreparationType preparationType;
    private AdductPreparationDetails adductPreparation;
    private HtpbBlendingPreparationDetails htpbBlendingPreparation;
    private List<SpecificationResponse> specifications;
    private List<CertificateResponse> certificates;
    private RmpProgressInsightsDto progressInsights;
    private RmpQualityInsightsDto qualityInsights;
    private RmpWorkflowInsightsDto workflowInsights;
    private Status status ;
    private String rejectionReason;
}
