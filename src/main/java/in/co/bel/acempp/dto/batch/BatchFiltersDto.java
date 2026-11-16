package in.co.bel.acempp.dto.batch;

import in.co.bel.acempp.enums.Status;
import lombok.Data;

import java.util.List;

@Data
public class BatchFiltersDto {
    private String search;
    private Status status;
    private String priority;
    private String department;
    private String subDepartment;
    private Integer motorStage;
    private List<String> motorIds;
    private List<String> lotIds;

    private String filterType;   // day, week, month, custom
    private String startDate;    // dd-MM-yyyy
    private String endDate;      // dd-MM-yyyy
}