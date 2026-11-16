package in.co.bel.acempp.dto.auth;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
public class LogoutResponse {
    private String userId;
    private LocalDateTime logoutTime;
}