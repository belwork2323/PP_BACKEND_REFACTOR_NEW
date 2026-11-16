package in.co.bel.acempp.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserUuidRequest {

    @NotBlank(message = "User UUID is required")
    private String user_uuid;
}
