package in.co.bel.acempp.dto.user;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateUserRequest {

    @NotBlank(message = "User ID is required")
    @Size(min = 3, max = 20, message = "User ID must be between 3 and 20 characters long")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "User ID must be alphanumeric only (no spaces or special characters)")
    private String userId;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters long")
    @Pattern(regexp = "^[A-Za-z\\s]+$", message = "Username must contain only alphabetic characters and spaces")
    private String username;

    @NotNull(message = "Role ID is required")
    private Long roleId;
//
//    @NotEmpty(message = "Sub Department IDs are required")
    private List<Long> subDepartmentIds;
}
