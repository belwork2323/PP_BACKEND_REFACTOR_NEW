package in.co.bel.acempp.dto.qcdivision;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.qcdivision.QcDivisionPayloads.DivisionDetailInput;
import in.co.bel.acempp.dto.qcdivision.QcDivisionPayloads.DivisionDetailInputDeserializer;
import in.co.bel.acempp.enums.Status;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateQcDivisionRequest {
    @NotBlank
    private String batchId;
    @NotNull
    private Long subDepartmentId;
    @NotNull
    private Status formSubmissionType;
    @NotEmpty
    @JsonDeserialize(contentUsing = DivisionDetailInputDeserializer.class)
    private List<DivisionDetailInput> divisionDetails;
}
