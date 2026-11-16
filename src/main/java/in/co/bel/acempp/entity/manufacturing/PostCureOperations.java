package in.co.bel.acempp.entity.manufacturing;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.postcureoperations.PostCureMotor;
import in.co.bel.acempp.enums.PostCureInhibitorType;
import in.co.bel.acempp.enums.PostCureOperationType;
import in.co.bel.acempp.enums.Status;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "post_cure_operations")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PostCureOperations {

    @Id
    private String id;

    @Indexed(unique = true)
    private String formId;                    // PCF-2026-001

    @Indexed(unique = true)                  // one form per batch
    private String batchId;

    private Long subDepartmentId;
    private Status formSubmissionType;        // DRAFT / SUBMIT
    private Status status;                    // DRAFT / WAITING_FOR_APPROVAL
    private PostCureOperationType operationType;
    private PostCureInhibitorType inhibitorType;

    private List<PostCureMotor> motors;

    private PersonInfo submittedBy;
    private LocalDateTime submittedAt;
    private PersonInfo updatedBy;
    private LocalDateTime updatedAt;
    private String remarks;
    private String rejectionReason;

    private boolean isDeleted = false;
}
