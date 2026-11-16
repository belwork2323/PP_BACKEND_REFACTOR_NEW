package in.co.bel.acempp.dto.qcdivision;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class QcDivisionDetailsResponse {
    private String formId;
    private String batchId;
    private Long subDepartmentId;
    private Status formSubmissionType;
    private PersonInfo submittedBy;
    private LocalDateTime submittedAt;
    private PersonInfo approvedBy;
    private LocalDateTime approvedOn;
    private String createdBy;
    private LocalDateTime createdAt;
    private Status status;
    private List<DivisionDetailDto> divisionDetails;
}
