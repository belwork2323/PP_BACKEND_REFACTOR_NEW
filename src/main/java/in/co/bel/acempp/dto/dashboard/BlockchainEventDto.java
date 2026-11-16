package in.co.bel.acempp.dto.dashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BlockchainEventDto {
    private String transactionId;
    private String batchId;
    private String eventType;
    private String assetId;
    private String eventStatusMessage;
    private String department;
    private String subDepartment;
    private String division;
    private String performedBy;
    private String timestamp;
    private Long blockNumber;
}