package in.co.bel.acempp.dto.general;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {
    private String projectId;
    private String projectName;
}
