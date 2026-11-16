package in.co.bel.acempp.dto.rocketmotorcasing;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.general.ProjectDto;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CasingResponse {

    private String motorCasingId;
    private ProjectDto project;
    private Integer motorStage;
    private String motorId;
    private String casingType;
    private String insulationType;
    private LocalDate receivingDate;
    private String status;
    private PersonInfo createdBy;
    private LocalDateTime createdOn;

}
