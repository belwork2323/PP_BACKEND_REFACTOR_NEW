package in.co.bel.acempp.dto.admin.motor;

import lombok.Data;

@Data
public class MotorListRequest {
    private Integer page;
    private Integer limit;
    private String search;
    private MotorListFiltersDto filters;
}
