package in.co.bel.acempp.dto.dispatch;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.enums.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdateDispatchRequest {
    @NotBlank private String formId;
    @NotBlank private String batchId;
    @NotNull private Long subDepartmentId;
    @NotNull private Status formSubmissionType;
    @NotEmpty
    @JsonDeserialize(using = DispatchMotorListDeserializer.class)
    private List<@Valid DispatchMotorRequest> motors;

    @JsonAnySetter
    public void rejectUnknownField(String field, Object ignored) {
        throw new IllegalArgumentException("Unknown Dispatch update field: " + field);
    }
}
