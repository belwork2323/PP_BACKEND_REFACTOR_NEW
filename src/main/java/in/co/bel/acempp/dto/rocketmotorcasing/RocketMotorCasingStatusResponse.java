package in.co.bel.acempp.dto.rocketmotorcasing;

import in.co.bel.acempp.enums.Status;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
public class RocketMotorCasingStatusResponse {

    private String motorCasingId;
    private Status status;
    private String remarks;
    private String rejectionReason;
    private String actionBy;
    private LocalDateTime actionAt;
}
