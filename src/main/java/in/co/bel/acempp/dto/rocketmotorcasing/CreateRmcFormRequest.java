package in.co.bel.acempp.dto.rocketmotorcasing;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
//import in.co.bel.acempp.enums.MotorStage;
import in.co.bel.acempp.enums.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRmcFormRequest {

    @NotNull(message = "subDepartmentId is required")
    private Long subDepartmentId;

    @NotBlank(message = "projectId is required")
    private String projectId;

    @NotNull(message = "motorStage is required")
    @Min(0)
    private Integer motorStage;

    @NotBlank(message = "motorId is required")
    private String motorId;

    @NotNull(message = "formSubmissionType is required")
    private Status formSubmissionType;

    @Valid
    @JsonDeserialize(using = RmcSectionsRequestDeserializer.class)
    private RmcSectionsRequest sections;

    @JsonAnySetter
    public void rejectUnknownField(String field, Object ignored) {
        throw new IllegalArgumentException("Unknown RMC create field: " + field);
    }
}
