package in.co.bel.acempp.dto.dashboard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class ManagerBlockchainEventsRequest {
    @NotBlank
    private String systemManagerId;
    private String search;
    private String eventType;
    private String department;
    private String subDepartment;
    private String startDate;
    private String endDate;
    @NotNull
    private int page;
    @NotNull private int pageSize;
}