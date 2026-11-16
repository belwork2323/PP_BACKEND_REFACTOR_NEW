package in.co.bel.acempp.dto.dashboard;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class StageProcessedDto {
    private String stage;
    private long   batchCount;
    private int    percentage;
}