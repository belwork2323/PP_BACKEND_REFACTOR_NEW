package in.co.bel.acempp.dto.batch;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BatchPaginationDto {
    @NotNull
    private int page;
    @NotNull private int limit;
}