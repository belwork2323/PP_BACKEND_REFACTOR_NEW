package in.co.bel.acempp.dto.user;

import lombok.Data;

@Data
public class UserStatsRequest {
    private String filterType;
    private String startDate;
    private String endDate;
}