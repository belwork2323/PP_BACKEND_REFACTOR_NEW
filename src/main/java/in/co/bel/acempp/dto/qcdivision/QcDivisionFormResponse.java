package in.co.bel.acempp.dto.qcdivision;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QcDivisionFormResponse {
    private String formId;
    private String batchId;
    private Long subDepartmentId;
    private Status formSubmissionType;
    private Status status;

    private List<DivisionStatusDto> divisionStatuses;
    private List<QcMotorStatusDto> motorStatuses;
    private List<QcPremixStatusDto> premixStatuses;
    private List<QcPremixStatusDto> finalMixStatuses;
    private List<DivisionDetailDto> divisionDetails;

    private PersonInfo createdBy;
    private LocalDateTime createdAt;
    private PersonInfo submittedBy;
    private LocalDateTime submittedAt;
    private PersonInfo approvedBy;
    private LocalDateTime approvedAt;
}
