package in.co.bel.acempp.entity;

import in.co.bel.acempp.enums.ResetRequestStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
@Document(collection = "password_reset_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {
    @Id
    private String id;                      // MongoDB ObjectId (not exposed)
    @Indexed(unique = true)
    private String requestId;               // business key (e.g., "PRR-00001")
    private String userId;                  // reference to User.userId
    private String reason;
    @Indexed
    private ResetRequestStatus status;      // PENDING, APPROVED, REJECTED
    private LocalDateTime requestedAt;
    private String reviewedByUserId;
    private String reviewedByName;
    private LocalDateTime resolvedAt;
    private String remarks;
}