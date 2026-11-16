package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;
import lombok.*;
@Data
@Builder
public class StatusCounts {
    private long initiated;
    private long inProgress;
    private long waitingForApproval;
    private long approved;
    private long rejected;
}
