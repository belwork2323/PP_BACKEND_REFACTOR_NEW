package in.co.bel.acempp.dto.batch;

import in.co.bel.acempp.dto.general.PersonInfo;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DeleteBatchResponse {
    private String batchId;
    private LocalDateTime deletedOn;
    private PersonInfo deletedBy;
    private String reason;
}