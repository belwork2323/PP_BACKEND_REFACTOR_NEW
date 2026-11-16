package in.co.bel.acempp.dto.batch;

import lombok.*;


@Data
@Builder
public class SubDeptStatsResponse {
    private long allocated;
    private long approved;
    private long rejected;
    private long pending;
}
