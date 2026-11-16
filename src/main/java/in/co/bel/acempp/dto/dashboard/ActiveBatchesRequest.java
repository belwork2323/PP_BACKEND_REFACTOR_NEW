package in.co.bel.acempp.dto.dashboard;

import lombok.Data;

@Data
public class ActiveBatchesRequest {
    private String search;
    private String stage;          // department or sub-department name, "All" for no filter
    private String type;           // batchType, "All" for no filter
    private String status;         // currentStatus, "All" for no filter
    private String startDate;      // yyyy-MM-dd
    private String endDate;        // yyyy-MM-dd
    private boolean currentMonth;  // if true, ignore start/end and use current month
    private String listType;       // e.g., "IN_PROGRESS/COMPLETED" or specific status groupings
    private int page = 1;
    private int pageSize = 10;
}