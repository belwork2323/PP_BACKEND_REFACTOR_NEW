package in.co.bel.acempp.dto.general;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolesDto {
    @NotNull(message = "Role ID is required")
    @Positive(message = "Role ID must be a positive number")
    private Long roleId;

    @NotBlank(message = "Role name is required")
    @Size(max = 50, message = "Role name must not exceed 50 characters")
    private String roleName;
}

