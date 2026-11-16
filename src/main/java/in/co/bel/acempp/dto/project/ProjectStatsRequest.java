package in.co.bel.acempp.dto.project;

import lombok.Data;

@Data
public class ProjectStatsRequest {

    private String filterType;

    private String startDate;
    private String endDate;
}