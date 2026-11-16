package in.co.bel.acempp.dto.rocketmotorcasing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableMotorResponse {

    private String motorCasingId;
    private String motorId;
    private String status;
}
