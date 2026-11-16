package in.co.bel.acempp.dto.rocketmotorcasing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotorsStatsResponse {
    private long createdMotors;
    private long pendingMotors;
    private long waitingForApprovalMotors;
    private long approvedMotors;
    private long rejectedMotors;
}


