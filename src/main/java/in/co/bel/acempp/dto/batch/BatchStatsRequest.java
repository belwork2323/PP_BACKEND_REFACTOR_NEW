package in.co.bel.acempp.dto.batch;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BatchStatsRequest {
    @NotBlank
    private String filterType;
    private String startDate;
    private String endDate;
}
