package in.co.bel.acempp.dto.auth;

import in.co.bel.acempp.dto.user.UserInfoDto;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private UserInfoDto user;
    private TokenDto token;
}
