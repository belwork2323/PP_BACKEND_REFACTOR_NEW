package in.co.bel.acempp.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank
    private String user_uuid;
    @NotBlank private String currentPassword;
    @NotBlank private String newPassword;
    @NotBlank private String confirmPassword;
}
