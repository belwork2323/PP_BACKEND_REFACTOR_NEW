package in.co.bel.acempp.dto.statictestingfacility;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.enums.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdateStfRequest {
    @NotBlank
    private String formId;
    @NotBlank
    private String batchId;
    @NotNull
    private Long subDepartmentId;
    @NotNull
    private Status formSubmissionType;

    @JsonDeserialize(using = StfMotorListDeserializer.class)
    private List<@Valid StfMotorRequest> motors;

    @JsonAnySetter
    public void rejectUnknownField(String field, Object ignored) {
        throw new IllegalArgumentException("Unknown STF update field: " + field);
    }
}
