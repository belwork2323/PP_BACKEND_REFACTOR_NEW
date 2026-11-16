package in.co.bel.acempp.dto.dashboard.systemmanagerdashboard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ManagerDashboardChartRequest {

    @NotBlank(message = "filterType is required")
    private String filterType;

    @Pattern(
            regexp = "^\\d{2}-\\d{2}-\\d{4}$",
            message = "startDate must be in DD-MM-YYYY format"
    )
    private String startDate;

    @Pattern(
            regexp = "^\\d{2}-\\d{2}-\\d{4}$",
            message = "endDate must be in DD-MM-YYYY format"
    )
    private String endDate;
}