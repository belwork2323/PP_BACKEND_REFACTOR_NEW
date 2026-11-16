package in.co.bel.acempp.dto.batch;

import lombok.Builder;
import lombok.Data;

// StatMetric.java
@Data
@Builder
public class StatMetric {
    private long count;
    private long subValue;
}