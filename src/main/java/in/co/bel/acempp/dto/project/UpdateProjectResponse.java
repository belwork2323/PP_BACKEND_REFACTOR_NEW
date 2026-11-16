package in.co.bel.acempp.dto.project;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProjectResponse {

    private String projectId;

    private String projectName;

    private String projectDescription;

    private String projectDate;

    private LocalDateTime createdOn;

    private LocalDateTime updatedOn;
}
