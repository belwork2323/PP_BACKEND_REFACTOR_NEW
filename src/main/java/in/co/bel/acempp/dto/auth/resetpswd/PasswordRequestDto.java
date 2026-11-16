package in.co.bel.acempp.dto.auth.resetpswd;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

// RequestDto.java
@Data
@Builder
public class PasswordRequestDto {
    private String requestId;
    private String userId;
    private String reason;
    private String status;
    private LocalDateTime requestedAt;
}
