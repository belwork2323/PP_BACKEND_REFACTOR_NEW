package in.co.bel.acempp.dto.dashboard.systemmanagerdashboard;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchStatusListRequest {

    @NotBlank(message = "batchId is required")
    private String batchId;

    @NotNull(message = "departmentId is required")
    private Long departmentId;
}