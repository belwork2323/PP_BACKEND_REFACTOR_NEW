package in.co.bel.acempp.entity.sourcing;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.rawmaterialsourcing.CertificateRequest;
import in.co.bel.acempp.dto.rawmaterialsourcing.preparation.AdductPreparationDetails;
import in.co.bel.acempp.dto.rawmaterialsourcing.preparation.HtpbBlendingPreparationDetails;
import in.co.bel.acempp.dto.user.operations.MaterialGrade;
import in.co.bel.acempp.enums.PreparationType;
import in.co.bel.acempp.enums.RawMaterialCategory;
import in.co.bel.acempp.enums.Status;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "lots")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Lot {
    @Id
    private String id;
    @Indexed(unique = true)
    private String lotId;               // e.g. "LOT-2026-0010"
    @Indexed
    private String sourcingId;       // reference to Procurement.procurementId
    private Long subDepartmentId;
    private String materialCode;
    private String materialName;        // denormalised for list views
    private MaterialGrade grade;
    private String supplyOrderNo;
    private LocalDate receiptDate;
    private String manufacturerName;
    private Status status;              // In Progress, Waiting for Approval, Approved, Rejected
    private Status submissionType;
    private String createdByUserId;      // e.g. "EMP3000"
    private String createdByUsername;
    private LocalDateTime createdAt;
    private PersonInfo updatedBy;
    private LocalDateTime updatedAt;
    private List<LotSpec> specifications;   // embedded
    private List<CertificateRequest> certificates;     // embedded
    private RawMaterialCategory rawMaterialType;
    private PreparationType preparationType;
    private AdductPreparationDetails adductPreparation;
    private HtpbBlendingPreparationDetails htpbBlendingPreparation;
    private String remarks;
    private String rejectionReason;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LotSpec {
        private String specificationCode;
        private String specificationName;
        private Double minValue;
        private Double maxValue;
        private Long unitId;
        private String unit;
        private String analysedResult;
        private boolean isOutOfRange;
        private String acemQcResult;
        private String status;          // Passed / Failed
    }

}
