package in.co.bel.acempp.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserRequest {

    @NotBlank(message = "User UUID is required")
    private String user_uuid;

    private String username;

    private List<Long> subDepartmentIds;
}
