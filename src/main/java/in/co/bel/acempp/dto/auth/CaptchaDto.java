

package in.co.bel.acempp.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaptchaDto {

    @NotBlank(message = "Captcha ID is required")
    @Size(max = 100, message = "Captcha ID is too long")
    private String captchaId;

    @NotBlank(message = "Captcha value is required")
    @Pattern(
            regexp = "^[a-zA-Z0-9]{4,8}$",
            message = "Captcha must be 4 to 8 alphanumeric characters"
    )
    private String captchaValue;
}