package in.co.bel.acempp.dto.dashboard;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DashboardStatsRequest {

    @NotBlank(message = "filterType is required")
    @Pattern(
            regexp = "^(day|week|month|custom)$",
            message = "filterType must be 'day', 'week', 'month', or 'custom'"
    )
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

    // Ensure custom dates are provided when filterType is custom
    @AssertTrue(message = "startDate and endDate are required when filterType is 'custom'")
    public boolean isCustomDateValid() {
        if ("custom".equalsIgnoreCase(filterType)) {
            return startDate != null && !startDate.isBlank()
                    && endDate != null && !endDate.isBlank();
        }
        return true;
    }
}