package in.co.bel.acempp.dto.qcdivision;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QcDivisionCreateResponse {
    private String formId;
    private String batchId;
    private Long subDepartmentId;
    private Status status;
    private List<DivisionStatusDto> divisionDetails;
}

