package in.co.bel.acempp.dto.rawmaterialsourcing;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RmpStatsRequest {
    @NotNull private Long subDepartmentId;
    @Min(1) @Max(12) private int month;
    @Min(2000) @Max(2100) private int year;
}