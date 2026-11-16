package in.co.bel.acempp.dto.project;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteProjectResponse {

    private String projectId;
    private LocalDateTime deletedOn;
}
