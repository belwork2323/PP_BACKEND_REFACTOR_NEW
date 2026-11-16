package in.co.bel.acempp.dto.user.operations;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

// UserBatchListRequest.java
@Data
public class UserBatchListRequest {
    private Long  subDepartmentId;
    private String  userId;
    @NotNull   private int    page;
    @NotNull   private int   limit;
    private List<String> status;
    private List<String> priority;
    private String       search;
    private List<String> batchIds;
    private List<String> batchTypes;
    private List<Object> motorStages;
    private List<String> motorIds;
    private List<String> formSubmittedBy;
    private LocalDate fromDate;
    private LocalDate toDate;
}