package in.co.bel.acempp.dto.auth.resetpswd;

import lombok.Builder;
import lombok.Data;

// ResetPasswordResponse.java
@Data
@Builder
public class ResetPasswordResponse {
    private PasswordRequestDto request;
}