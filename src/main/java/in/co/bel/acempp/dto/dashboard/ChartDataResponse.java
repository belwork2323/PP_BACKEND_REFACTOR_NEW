package in.co.bel.acempp.dto.dashboard;

import lombok.Builder;
import lombok.Data;

import java.util.List;

// ChartDataResponse.java
@Data
@Builder
public class ChartDataResponse {
    private String filterType;
    private String timeUnit;
    private List<ChartPoint> weeklyActivity;
    private List<ChartPoint> motorsProcessed;
    private List<ChartPoint> qcPassRate;
}