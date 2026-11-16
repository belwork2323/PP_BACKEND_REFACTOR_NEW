package in.co.bel.acempp.dto.project;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ProjectListRequest {
    @NotNull(message = "page is required")
    @Min(value = 1, message = "page must be at least 1")
    private Integer page;

    @NotNull(message = "limit is required")
    @Min(value = 1, message = "limit must be at least 1")
    private Integer limit;

    private String search;
    private String startDate; // YYYY-MM-DD
    private String endDate;   // YYYY-MM-DD
    private String sortBy;
    private String sortOrder;
}

