package in.co.bel.acempp.dto.dashboard;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AlertsResponse {
    private String systemManagerId;
    private List<AlertDto> alerts;
    private AlertSummaryDto summary;
    private int page;
    private int limit;
}