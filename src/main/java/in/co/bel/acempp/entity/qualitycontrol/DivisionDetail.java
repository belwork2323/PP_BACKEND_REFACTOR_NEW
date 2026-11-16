package in.co.bel.acempp.entity.qualitycontrol;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder @NoArgsConstructor @AllArgsConstructor
public class DivisionDetail {
    private Long divisionId;
    private String division;           // e.g. RAW_MATERIAL_REVALIDATION
    private String subType;            // optional, e.g. MAIN_BATCH
    private Status divisionSubmissionType;
    private Status divisionSubmissionStatus;
    private Status status;
    private String remarks;
    private String rejectionReason;
    private PersonInfo submittedBy;
    private LocalDateTime submittedAt;
    private PersonInfo approvedBy;
    private LocalDateTime approvedAt;
    // Retained for compatibility with existing persisted approval records.
    private PersonInfo actionBy;
    private LocalDateTime actionAt;
    private Map<String, Object> data;  // flexible content
}
