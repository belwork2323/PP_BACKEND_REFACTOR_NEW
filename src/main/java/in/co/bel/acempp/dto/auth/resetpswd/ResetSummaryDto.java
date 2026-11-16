package in.co.bel.acempp.dto.auth.resetpswd;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResetSummaryDto {
    private long all;
    private long pending;
    private long approved;
    private long rejected;
}