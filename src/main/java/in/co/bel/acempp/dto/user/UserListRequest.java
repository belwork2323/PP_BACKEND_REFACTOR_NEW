package in.co.bel.acempp.dto.user;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserListRequest {

    private String search;

    private String role;

    private String department;

    private Long subDepartmentId;

    private String status;

    private String filterType;
    private String startDate;
    private String endDate;



    @NotNull(message = "page is required")
    @Min(value = 1, message = "page must be greater than 0")
    private Integer page;

    @NotNull(message = "pageSize is required")
    @Min(value = 1, message = "pageSize must be greater than 0")
    private Integer pageSize;
}
