package in.co.bel.acempp.dto.dashboard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AlertsRequest {
    @NotBlank
    private String systemManagerId;
    private List<String> type;
    @NotNull
    private int page;
    @NotNull private int limit;
    private String search;
    private DateRangeDto dateRange;
}