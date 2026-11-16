package in.co.bel.acempp.entity.manufacturing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.enums.Status;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "raw_material_preparations")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RawMaterialPreparation {

    @Id
    private String id;

    @Indexed(unique = true)
    private String formId;                     // e.g. RMPF-2026-001

    @Indexed(unique = true)                   // one form per batch
    private String batchId;

    private LocalDate premixDate;

    private Long subDepartmentId;
    private Status formSubmissionType;         // DRAFT / SUBMIT
    private Status status;                     // DRAFT / WAITING_FOR_APPROVAL
    private PreparationDetails preparationDetails;
    private PersonInfo submittedBy;            // createdBy
    private LocalDateTime submittedAt;
    private PersonInfo updatedBy;
    private LocalDateTime updatedAt;
    private boolean isDeleted = false;
    private String remarks;
    private String rejectionReason;

}
