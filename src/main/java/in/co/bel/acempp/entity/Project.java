package in.co.bel.acempp.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "projects")
public class Project {

    @Id
    private String id;

    @Indexed(unique = true)
    private String projectId;

    @Indexed(unique = true)
    private String projectName;

    private String projectDescription;

    private LocalDate projectDate;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder.Default
    private Boolean isDeleted = false;

    private LocalDateTime deletedAt;
}