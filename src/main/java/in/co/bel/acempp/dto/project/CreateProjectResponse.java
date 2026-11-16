package in.co.bel.acempp.dto.project;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProjectResponse {

    private String projectId;

    private String projectName;

    private String projectDescription;

    private LocalDate projectDate;

    private LocalDateTime createdOn;
}
