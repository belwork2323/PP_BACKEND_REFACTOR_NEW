package in.co.bel.acempp.dto.dashboard;

import in.co.bel.acempp.enums.Status;
import lombok.Data;

@Data
public class BlockchainEventsRequest {
    private String search;
    private String eventType;
    private String subDepartment;
    private String startDate;    // yyyy-MM-dd
    private String endDate;      // yyyy-MM-dd
    private int page = 1;
    private int pageSize = 10;
    private String status;
}