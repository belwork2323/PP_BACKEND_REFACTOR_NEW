package in.co.bel.acempp.dto.auth.resetpswd;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

// ResetPasswordRequest.java
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "userId is required")
    @Size(min = 3, max = 50, message = "userId must be between 3 and 50 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9_-]+$",
            message = "userId must contain only letters, numbers, hyphens, or underscores"
    )
    private String userId;

    @NotBlank(message = "reason is required")
    @Size(min = 10, max = 500, message = "reason must be between 10 and 500 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9 .,!?'\\-()]+$",
            message = "reason may only contain letters, numbers, spaces, and . , ! ? ' - ( )"
    )
    private String reason;
}
