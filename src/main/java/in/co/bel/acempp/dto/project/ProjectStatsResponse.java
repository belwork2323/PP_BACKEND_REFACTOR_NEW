package in.co.bel.acempp.dto.project;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStatsResponse {

    private long totalProjects;

    private long projectsCreatedToday;

    private long projectsCreatedThisMonth;

    private long activeProjects;

    private long idleProjects;
}