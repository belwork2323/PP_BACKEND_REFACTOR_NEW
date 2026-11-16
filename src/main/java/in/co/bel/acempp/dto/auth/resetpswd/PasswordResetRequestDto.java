package in.co.bel.acempp.dto.auth.resetpswd;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PasswordResetRequestDto {
    private String requestId;
    private String userId;
    private String reason;
    private String status;
    private LocalDateTime requestedAt;
    private ReviewedByDto reviewedBy;  // null for PENDING
    private LocalDateTime reviewedAt;         // null for PENDING
    private String remarks;            // null for PENDING
    /** Present when APPROVED and user has not yet changed the temp password. */
    private String temporaryPassword;
}