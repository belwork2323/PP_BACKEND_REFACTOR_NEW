package in.co.bel.acempp.dto.rawmaterialsourcing;
import lombok.*;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LotStatsResponse {

    private long createdLots;
    private long pendingLots;
    private long waitingForApprovalLots;
    private long approvedLots;
    private long rejectedLots;
}
