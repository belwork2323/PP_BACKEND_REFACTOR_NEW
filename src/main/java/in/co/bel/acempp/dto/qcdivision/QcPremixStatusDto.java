package in.co.bel.acempp.dto.qcdivision;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QcPremixStatusDto {
    private String division;
    private String subType;
    private String stageType;
    private Integer premixNo;
    private Status premixSubmissionType;
    private Status premixSubmissionStatus;
    private String remarks;
    private String rejectionReason;
    private PersonInfo submittedBy;
    private LocalDateTime submittedAt;
    private PersonInfo approvedBy;
    private LocalDateTime approvedAt;
}
