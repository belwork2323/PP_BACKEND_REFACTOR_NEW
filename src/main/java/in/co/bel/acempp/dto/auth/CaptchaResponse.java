package in.co.bel.acempp.dto.auth;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponse {
    private String captchaId;
    private String captchaImage;  // base64 PNG
    private int expiresIn;        // seconds
}