package in.co.bel.acempp.dto.rocketmotorcasing;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.general.ProjectDto;
//import in.co.bel.acempp.enums.MotorStage;
import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RmcDetailsResponse {
    private String motorCasingId;
    private ProjectDto project;
    private Long subDepartmentId;
    private Integer motorStage;
    private String motorId;
    private Status status;
    /** Flattened from sections.motorReceipt for identification header UIs. */
    private String casingType;
    private String insulationType;
    private LocalDate receivingDate;
    /** User id (legacy); prefer {@link #createdByInfo} for display name. */
    private String createdBy;
    private PersonInfo createdByInfo;
    private LocalDateTime createdAt;
    /** Alias of createdAt for list/detail UI parity. */
    private LocalDateTime createdOn;
    private LocalDateTime updatedAt;
    private RmcSectionsResponse sections;
}
