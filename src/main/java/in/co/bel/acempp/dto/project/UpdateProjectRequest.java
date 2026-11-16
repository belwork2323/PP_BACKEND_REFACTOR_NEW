package in.co.bel.acempp.dto.project;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProjectRequest {

    @NotBlank(message = "projectId is required")
    private String projectId;
    private String projectName;
    private String projectDescription;
}
