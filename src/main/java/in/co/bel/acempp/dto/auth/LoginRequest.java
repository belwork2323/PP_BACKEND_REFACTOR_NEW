package in.co.bel.acempp.dto.auth;

import in.co.bel.acempp.dto.general.RolesDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "User ID is required")
    @Size(min = 4, max = 50, message = "User ID must be between 4 and 50 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9._@-]+$",
            message = "User ID can contain only letters, numbers, ., _, @ and -"
    )
    private String userId;

//    @NotBlank(message = "Password is required")
//    @Pattern(
//            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
//            message = "Password must be at least 8 characters long and include uppercase, lowercase, number, and special character"
//    )
    private String password;

    @NotNull(message = "Role is required")
    @Valid
    private RolesDto role;

    @NotNull(message = "Captcha is required")
    @Valid
    private CaptchaDto captcha;
}
