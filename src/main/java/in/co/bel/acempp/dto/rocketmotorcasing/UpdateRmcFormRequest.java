package in.co.bel.acempp.dto.rocketmotorcasing;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
//import in.co.bel.acempp.enums.MotorStage;
import in.co.bel.acempp.enums.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRmcFormRequest {

    @NotBlank
    private String motorCasingId;

    // OPTIONAL FOR PATCH
    private String projectId;

    // OPTIONAL FOR PATCH
    private String motorId;

    // OPTIONAL FOR PATCH
    private Long subDepartmentId;

    // OPTIONAL FOR PATCH
    private Integer motorStage;

    // REQUIRED
    @NotNull
    private Status formSubmissionType;

    // OPTIONAL FOR PATCH
    @Valid
    @JsonDeserialize(using = RmcSectionsRequestDeserializer.class)
    private RmcSectionsRequest sections;

    @JsonAnySetter
    public void rejectUnknownField(String field, Object ignored) {
        throw new IllegalArgumentException("Unknown RMC update field: " + field);
    }
}
