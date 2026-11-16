package in.co.bel.acempp.dto.general;

import jakarta.validation.constraints.NotNull;
import lombok.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsRequest {

    @NotNull(message = "Sub department id is required")
    private Long subDepartmentId;
}
