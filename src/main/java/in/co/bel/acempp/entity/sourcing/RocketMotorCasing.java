package in.co.bel.acempp.entity.sourcing;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.Sections;
//import in.co.bel.acempp.enums.MotorStage;
import in.co.bel.acempp.enums.Status;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;


import java.time.LocalDateTime;

@Document(collection = "rocket_motor_casing")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RocketMotorCasing {
    @Id
    private String id;
    private Long subDepartmentId;
    private String projectId;                  // nullable for EXPERIMENTAL
    private String projectName;
    private Integer motorStage;
    // USER ENTERED
    private String motorId;
    // AUTO GENERATED ON SUBMIT
    @Indexed(unique = true, sparse = true)
    private String motorCasingId;
    // DRAFT / SUBMIT
    private Status formSubmissionType;
    // IN_PROGRESS / WAITING_FOR_APPROVAL / APPROVED
    private Status status;
    private Sections sections;

    private Boolean active;
//    private PersonInfo createdBy;
    private String createdByUserId;      // e.g. "EMP3000"
    private String createdByUsername;
    private LocalDateTime createdAt;
    private PersonInfo updatedBy;
    private String nextStep;
    private LocalDateTime updatedAt;
    private String remarks;
    private String rejectionReason;
}