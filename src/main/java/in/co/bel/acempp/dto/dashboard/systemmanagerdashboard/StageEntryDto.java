package in.co.bel.acempp.dto.dashboard.systemmanagerdashboard;

import in.co.bel.acempp.dto.general.PersonInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageEntryDto {

    private Long subDepartmentId;

    private String subDepartmentName;

    private String status;

    /** Form document id for this stage, when a form exists. */
    private String formId;

    private PersonInfo submittedBy;

    private LocalDateTime submittedOn;

    private PersonInfo approvedBy;

    private LocalDateTime approvedOn;

    private String remarks;

    private String rejectionReason;
}