package in.co.bel.acempp.dto.project;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteProjectRequest {

    @NotBlank(message = "Project ID is required")
    private String projectId;
}

