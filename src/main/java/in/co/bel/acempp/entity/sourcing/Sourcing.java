
package in.co.bel.acempp.entity.sourcing;

import in.co.bel.acempp.enums.Status;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
@Document(collection = "sourcing")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Sourcing {
    @Id
    private String id;                  // MongoDB ObjectId
    @Indexed(unique = true)
    private String sourcingId;       // e.g. "RMP-2026-0001"
    private Long subDepartmentId;
    private Status submissionType;      // DRAFT, SUBMIT
    private Status status;              // In Progress, Waiting for Approval, Approved, Rejected
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}