package in.co.bel.acempp.dto.project;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ProjectDataDTO {
    private String projectId;
    private String projectName;
    private String projectDescription;
    private String projectDate;
    private LocalDateTime createdOn;
}