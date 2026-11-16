package in.co.bel.acempp.dto.batch.batchdetails;

import lombok.*;

@Data
@Builder
public class DelayInsights {
    private boolean isDelayed;
    private int delayInHours;
    private String delayReason;
}