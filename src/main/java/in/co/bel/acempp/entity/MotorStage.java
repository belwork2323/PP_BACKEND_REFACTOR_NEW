package in.co.bel.acempp.entity;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.service.masterdata.MasterDataAuditable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "motor_stage")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MotorStage implements MasterDataAuditable {

    @Id
    private String id;

    private Long stageId;

    private Integer motorStage;

    private String motorStageName;
    private String projectId;

    /** Null treated as active for documents created before soft-disable. */
    private Boolean isActive;

    private PersonInfo createdBy;
    private LocalDateTime createdOn;
    private PersonInfo updatedBy;
    private LocalDateTime updatedOn;
}
