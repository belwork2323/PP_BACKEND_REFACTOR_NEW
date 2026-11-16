package in.co.bel.acempp.dto.casepreparation;

import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
public class CasePreparationUpdateResponse {
    private String formId;
    private String batchId;
    private String batchType;
    private Status status;
    private String updatedBy;
    private LocalDateTime updatedAt;
}