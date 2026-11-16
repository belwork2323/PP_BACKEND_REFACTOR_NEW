package in.co.bel.acempp.dto.rawmaterialsourcing;

import in.co.bel.acempp.enums.Status;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LotListRequest {

    @NotNull(message = "subDepartmentId is required")
    private Long subDepartmentId;

    @NotNull(message = "page is required")
    @Min(value = 1, message = "page must be at least 1")
    private Integer page;

    @NotNull(message = "limit is required")
    @Min(value = 1, message = "limit must be at least 1")
    private Integer limit;

    private List<Status> status;        // optional
    private List<String> materialCode;  // optional
    private String manufacturerName;    // optional
    private String search;              // optional
    private String fromDate;            // optional - YYYY-MM-DD
    private String toDate;              // optional - YYYY-MM-DD
}