package in.co.bel.acempp.dto.auth.resetpswd;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChangeResetStatusResponse {
    private String requestId;
    private String status;
    private ReviewedByDto reviewedBy;
    private LocalDateTime reviewedAt;
    private String remarks;
    /** One-time temp password returned on APPROVE only. */
    private String temporaryPassword;
}