package in.co.bel.acempp.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChangePasswordResponse {
    private String user_uuid;
    private String username;
    private String status;
    private LocalDateTime passwordUpdatedOn;
}
