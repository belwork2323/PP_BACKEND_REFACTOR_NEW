package in.co.bel.acempp.dto.qcdivision;

import lombok.*;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.enums.Status;

import java.time.LocalDateTime;

@Data
@Builder
public class DivisionDetailDto {
    private Long divisionId;
    private String division;
    private String subType;
    private Status divisionSubmissionType;
    private Status divisionSubmissionStatus;
    private Status status;
    private String remarks;
    private String rejectionReason;
    private PersonInfo actionBy;
    private LocalDateTime actionAt;
    private PersonInfo submittedBy;
    private LocalDateTime submittedAt;
    private PersonInfo approvedBy;
    private LocalDateTime approvedAt;
    private QcDivisionPayloads.DivisionDataResponse data;
}
