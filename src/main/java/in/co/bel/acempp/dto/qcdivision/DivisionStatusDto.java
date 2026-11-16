package in.co.bel.acempp.dto.qcdivision;


import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
public class DivisionStatusDto {
    private Long divisionId;
    private String division;
    private Integer sequence;
    private PersonInfo unlockedBy;
    private LocalDateTime unlockedAt;
    private PersonInfo submittedBy;
    private LocalDateTime submittedAt;
    private PersonInfo approvedBy;
    private LocalDateTime approvedAt;
    private Status status;
    private boolean allUnitsApproved;
    private int pendingUnitCount;
    private int approvedUnitCount;
    private int rejectedUnitCount;
    private int waitingForApprovalUnitCount;
    private int inProgressUnitCount;
    private int totalUnitCount;
}
