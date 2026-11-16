package in.co.bel.acempp.dto.dashboard;

import lombok.Builder;
import lombok.Data;

// ChartPoint.java
@Data
@Builder
public class ChartPoint {
    private String label;
    private long value;
}
