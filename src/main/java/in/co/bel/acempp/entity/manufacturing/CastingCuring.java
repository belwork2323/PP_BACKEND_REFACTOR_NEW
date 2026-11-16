package in.co.bel.acempp.entity.manufacturing;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.castingcuring.CastingCuringMotor;
import in.co.bel.acempp.enums.Status;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "casting_curing")
@Data @Builder @NoArgsConstructor
@AllArgsConstructor
public class CastingCuring {

    @Id
    private String id;

    @Indexed(unique = true)
    private String formId;                         // CCF-2026-001

    @Indexed(unique = true)                       // one form per batch
    private String batchId;

    private Long subDepartmentId;                  // must match batch's current stage
    private Status formSubmissionType;             // DRAFT / SUBMIT
    private Status status;                         // DRAFT / WAITING_FOR_APPROVAL

    private List<CastingCuringMotor> motors;

    private PersonInfo submittedBy;
    private LocalDateTime submittedAt;
    private PersonInfo updatedBy;
    private LocalDateTime updatedAt;
    private String remarks;
    private String rejectionReason;

    private boolean isDeleted = false;
}
