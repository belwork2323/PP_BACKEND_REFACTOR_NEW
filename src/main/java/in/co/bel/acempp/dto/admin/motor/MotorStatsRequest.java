package in.co.bel.acempp.dto.admin.motor;

import lombok.Data;

@Data
public class MotorStatsRequest {
    private String filterType;
    private String startDate;
    private String endDate;
}
