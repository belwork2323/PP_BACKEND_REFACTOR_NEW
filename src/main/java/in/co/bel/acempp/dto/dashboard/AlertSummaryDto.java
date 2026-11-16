package in.co.bel.acempp.dto.dashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlertSummaryDto {
    private long total;
    private long error;
    private long warning;
    private long info;
}