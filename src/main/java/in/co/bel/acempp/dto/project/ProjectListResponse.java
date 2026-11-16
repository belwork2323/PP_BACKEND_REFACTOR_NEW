package in.co.bel.acempp.dto.project;

import in.co.bel.acempp.dto.general.PaginationDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProjectListResponse {
    private List<ProjectDataDTO> projects;
    private PaginationDto pagination;
}
