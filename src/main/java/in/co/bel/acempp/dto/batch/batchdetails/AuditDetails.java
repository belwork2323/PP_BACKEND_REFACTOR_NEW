package in.co.bel.acempp.dto.batch.batchdetails;

import in.co.bel.acempp.dto.general.PersonInfo;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditDetails {
    private PersonInfo submittedBy;
    private LocalDateTime submittedOn;
    private PersonInfo approvedBy;
    private LocalDateTime approvedOn;
    private PersonInfo rejectedBy;   // new
    private LocalDateTime rejectedOn;
}