package in.co.bel.acempp.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResponse {

    private long totalUsers;

    private long activeUsers;

    private long inactiveUsers;

    private long pendingResetRequests;
}
