package in.co.bel.acempp.entity.manufacturing;

import in.co.bel.acempp.dto.casepreparation.CasePreparationDetailsRequest;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.enums.Status;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "case_preparations")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CasePreparation {

    @Id
    private String id;

    @Indexed(unique = true)
    private String formId;                   // CPF-2026-001

    @Indexed(unique = true)                 // one form per batch
    private String batchId;

    private Long subDepartmentId;
    private Status formSubmissionType;      // DRAFT / SUBMIT
    private Status status;                  // DRAFT / WAITING_FOR_APPROVAL

    private CasePreparationDetailsRequest casePreparationDetails;

    private PersonInfo submittedBy;
    private LocalDateTime submittedAt;
    private PersonInfo updatedBy;
    private LocalDateTime updatedAt;
    private String remarks;
    private String rejectionReason;

    private boolean isDeleted = false;
}
